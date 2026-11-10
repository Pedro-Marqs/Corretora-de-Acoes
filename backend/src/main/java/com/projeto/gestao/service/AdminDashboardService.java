package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

import com.projeto.gestao.domain.dto.AdminDashboardAccountCreatedProjection;
import com.projeto.gestao.domain.dto.AdminDashboardMovementDayProjection;
import com.projeto.gestao.domain.dto.AdminDashboardMovementSummaryProjection;
import com.projeto.gestao.domain.dto.AdminDashboardRanking;
import com.projeto.gestao.domain.dto.AdminDashboardRankingProjection;
import com.projeto.gestao.domain.dto.AdminDashboardResponse;
import com.projeto.gestao.domain.dto.AdminDashboardSeriesPoint;
import com.projeto.gestao.domain.dto.AdminDashboardVolume;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.FinancialAmount;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.MovementRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private static final String USD_BRL = "USD/BRL";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final AccountRepository accounts;
    private final MovementRepository movements;
    private final ExchangeRateRepository exchangeRates;
    private final MarketDataFreshness freshness;
    private final Clock clock;

    public AdminDashboardService(AccountRepository accounts, MovementRepository movements,
            ExchangeRateRepository exchangeRates, MarketDataFreshness freshness, Clock clock) {
        this.accounts = accounts;
        this.movements = movements;
        this.exchangeRates = exchangeRates;
        this.freshness = freshness;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AdminDashboardResponse query(DashboardPeriod period, int limit) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime start = period.start(now, firstRelevantAt(now));

        Set<AccountStatus> includedStatuses = Set.of(AccountStatus.ACTIVE, AccountStatus.INACTIVE);
        long totalAccounts = accounts.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatusIn(
                start, now, includedStatuses);
        long activeAccounts = accounts.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatus(
                start, now, AccountStatus.ACTIVE);
        long inactiveAccounts = accounts.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatus(
                start, now, AccountStatus.INACTIVE);
        long newAccounts = totalAccounts;

        Map<MovementType, MovementTotals> totals = summarize(start, now);
        List<AdminDashboardSeriesPoint> series = series(start, now);
        List<AdminDashboardRanking> ranking = ranking(start, now, limit);

        return new AdminDashboardResponse(
                totalAccounts, activeAccounts, inactiveAccounts, newAccounts,
                totals.get(MovementType.PURCHASE).operationCount(),
                totals.get(MovementType.SALE).operationCount(),
                totals.get(MovementType.PURCHASE).shares(),
                totals.get(MovementType.SALE).shares(),
                volume(totals.get(MovementType.PURCHASE)),
                volume(totals.get(MovementType.SALE)), series, ranking);
    }

    private OffsetDateTime firstRelevantAt(OffsetDateTime now) {
        OffsetDateTime accountAt = accounts.findFirstCreatedAt();
        OffsetDateTime movementAt = movements.findFirstOccurredAt();
        if (accountAt == null) return movementAt == null ? now : movementAt;
        if (movementAt == null) return accountAt;
        return accountAt.isBefore(movementAt) ? accountAt : movementAt;
    }

    private Map<MovementType, MovementTotals> summarize(OffsetDateTime start, OffsetDateTime end) {
        Map<MovementType, MovementTotals> totals = new EnumMap<>(MovementType.class);
        totals.put(MovementType.PURCHASE, MovementTotals.empty());
        totals.put(MovementType.SALE, MovementTotals.empty());
        for (AdminDashboardMovementSummaryProjection row : movements
                .summarizeAdministrativeDashboard(start, end, MovementType.PURCHASE,
                        MovementType.SALE, Currency.BRL)) {
            totals.compute(row.getMovementType(), (ignored, current) -> current.add(
                    row.getCurrency(), row.getOperationCount(), row.getShares(),
                    row.getNativeVolume()));
        }
        return totals;
    }

    private List<AdminDashboardSeriesPoint> series(OffsetDateTime start, OffsetDateTime end) {
        Map<LocalDate, SeriesAccumulator> points = new TreeMap<>();
        for (AdminDashboardAccountCreatedProjection row : accounts.findCreatedAtBetween(start, end)) {
            points.computeIfAbsent(localDate(row.getCreatedAt()), ignored -> new SeriesAccumulator())
                    .newAccounts++;
        }
        for (AdminDashboardMovementDayProjection row : movements.findAdministrativeDashboardDays(
                start, end, MovementType.PURCHASE, MovementType.SALE)) {
            SeriesAccumulator accumulator = points.computeIfAbsent(
                    localDate(row.getOccurredAt()), ignored -> new SeriesAccumulator());
            if (row.getMovementType() == MovementType.PURCHASE) {
                accumulator.purchaseCount++;
            } else {
                accumulator.saleCount++;
            }
        }
        return points.entrySet().stream()
                .map(entry -> new AdminDashboardSeriesPoint(entry.getKey(),
                        entry.getValue().newAccounts, entry.getValue().purchaseCount,
                        entry.getValue().saleCount))
                .toList();
    }

    private List<AdminDashboardRanking> ranking(OffsetDateTime start, OffsetDateTime end, int limit) {
        return movements.findAdministrativeDashboardRanking(start, end,
                        MovementType.PURCHASE, MovementType.SALE, PageRequest.of(0, limit)).stream()
                .map(this::ranking)
                .toList();
    }

    private AdminDashboardRanking ranking(AdminDashboardRankingProjection row) {
        return new AdminDashboardRanking(row.getTicker(), row.getMarket(), row.getTradedShares());
    }

    private AdminDashboardVolume volume(MovementTotals totals) {
        BigDecimal brl = normalize(totals.brl());
        BigDecimal usd = normalize(totals.usd());
        if (usd.signum() == 0) {
            return new AdminDashboardVolume(brl, usd, null, "NOT_REQUIRED", null, null);
        }
        ExchangeRate rate = exchangeRates.findById(USD_BRL).orElse(null);
        if (rate == null || rate.getRate() == null || rate.getRate().signum() <= 0
                || rate.getQuotedAt() == null || rate.isStale()
                || freshness.exchangeRateIsStale(rate.getQuotedAt().toInstant())) {
            return new AdminDashboardVolume(brl, usd, null, "UNAVAILABLE", null, null);
        }
        BigDecimal normalizedRate = normalize(rate.getRate());
        BigDecimal equivalent = brl.add(new FinancialAmount(usd)
                .convertUsdToBrl(normalizedRate).value());
        return new AdminDashboardVolume(brl, usd, equivalent, "AVAILABLE", normalizedRate,
                brasilia(rate.getQuotedAt()));
    }

    private static LocalDate localDate(OffsetDateTime value) {
        return value.atZoneSameInstant(java.time.ZoneId.of("America/Sao_Paulo")).toLocalDate();
    }

    private static OffsetDateTime brasilia(OffsetDateTime value) {
        return value.atZoneSameInstant(java.time.ZoneId.of("America/Sao_Paulo"))
                .toOffsetDateTime();
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? ZERO : new FinancialAmount(value).value();
    }

    private record MovementTotals(long operationCount, long shares, BigDecimal brl,
            BigDecimal usd) {
        static MovementTotals empty() {
            return new MovementTotals(0, 0, ZERO, ZERO);
        }

        MovementTotals add(Currency currency, Long operations, Long quantity, BigDecimal volume) {
            BigDecimal safeVolume = normalize(volume);
            return new MovementTotals(operationCount + (operations == null ? 0 : operations),
                    shares + (quantity == null ? 0 : quantity),
                    currency == Currency.BRL ? brl.add(safeVolume) : brl,
                    currency == Currency.USD ? usd.add(safeVolume) : usd);
        }
    }

    private static final class SeriesAccumulator {
        private long newAccounts;
        private long purchaseCount;
        private long saleCount;
    }
}
