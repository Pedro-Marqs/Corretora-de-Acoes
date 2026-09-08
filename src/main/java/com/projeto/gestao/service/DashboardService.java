package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.AuthorizationException;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.AssociationStatus;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.FinancialAmount;
import com.projeto.gestao.domain.model.InvestmentResults;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.PositionFinancialCalculator;
import com.projeto.gestao.domain.model.PositionValuation;
import com.projeto.gestao.domain.model.Quote;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final String USD_BRL = "USD/BRL";
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private final AccountRepository accounts;
    private final AccountBrokerRepository accountBrokers;
    private final PositionRepository positions;
    private final MovementRepository movements;
    private final QuoteRepository quotes;
    private final ExchangeRateRepository exchangeRates;
    private final MarketDataFreshness freshness;

    public DashboardService(AccountRepository accounts, AccountBrokerRepository accountBrokers,
            PositionRepository positions,
            MovementRepository movements, QuoteRepository quotes,
            ExchangeRateRepository exchangeRates, MarketDataFreshness freshness) {
        this.accounts = accounts;
        this.accountBrokers = accountBrokers;
        this.positions = positions;
        this.movements = movements;
        this.quotes = quotes;
        this.exchangeRates = exchangeRates;
        this.freshness = freshness;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardSnapshot query(UUID accountId) {
        return query(accountId, null);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardSnapshot query(UUID accountId, UUID brokerAssociationId) {
        if (accountId == null) {
            throw new AuthenticationException();
        }
        Account account = accounts.findByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
        AccountBroker selectedBroker = brokerAssociationId == null ? null : accountBrokers
                .findByIdAndAccountIdAndStatus(
                        brokerAssociationId, accountId, AssociationStatus.ACTIVE)
                .orElseThrow(AuthorizationException::new);
        List<Position> openPositions = positions
                .findByAccountIdAndQuantityGreaterThan(accountId, 0L).stream()
                .filter(position -> selectedBroker == null
                        || position.getAccountBroker().getId().equals(selectedBroker.getId()))
                .toList();
        ExchangeRate rate = requiresUsd(openPositions) ? usableExchangeRate() : null;
        List<PositionValuation> valuations = new ArrayList<>();
        List<DashboardPositionView> positionViews = new ArrayList<>();
        List<DashboardWarningView> warnings = new ArrayList<>();
        Map<String, DistributionAccumulator> byAsset = new TreeMap<>();
        Map<String, DistributionAccumulator> byBroker = new TreeMap<>();
        Map<String, DistributionAccumulator> byMarket = new TreeMap<>();

        for (Position position : openPositions) {
            Quote quote = usableQuote(position);
            boolean quoteStale = quote.isStale()
                    || freshness.quoteIsStale(quote.getQuotedAt().toInstant());
            if (quoteStale) {
                warnings.add(new DashboardWarningView(DashboardWarningType.STALE_QUOTE,
                        position.getAsset().getTicker(), brasilia(quote.getQuotedAt())));
            }
            FinancialAmount nativePrice = new FinancialAmount(quote.getPrice());
            PositionValuation valuation = position.getAsset().getCurrency() == Currency.USD
                    ? PositionValuation.usd(position.financialBalance(), nativePrice, rate.getRate())
                    : PositionValuation.brl(position.financialBalance(), nativePrice);
            valuations.add(valuation);
            FinancialAmount priceBrl = position.getAsset().getCurrency() == Currency.USD
                    ? nativePrice.convertUsdToBrl(rate.getRate()) : nativePrice;
            FinancialAmount marketValue = PositionFinancialCalculator.marketValue(valuation);
            accumulate(byAsset, position.getAsset().getId().toString(),
                    position.getAsset().getTicker(), marketValue);
            accumulate(byBroker, position.getAccountBroker().getId().toString(),
                    position.getAccountBroker().getBroker().getTradeName(), marketValue);
            accumulate(byMarket, position.getAsset().getMarket().name(),
                    position.getAsset().getMarket().name(), marketValue);
            positionViews.add(new DashboardPositionView(
                    position.getAsset().getTicker(), position.getAsset().getName(),
                    position.getAsset().getMarket(), position.getAsset().getCurrency(),
                    position.getAccountBroker().getBroker().getTradeName(), position.getQuantity(),
                    position.getAveragePrice(), position.getTotalCost(), quote.getPrice(),
                    priceBrl.value(), marketValue.value(),
                    marketValue.subtract(new FinancialAmount(position.getTotalCost())).value(),
                    brasilia(quote.getQuotedAt()), quoteStale));
        }

        DashboardExchangeRateView rateView = null;
        if (rate != null) {
            boolean stale = rate.isStale()
                    || freshness.exchangeRateIsStale(rate.getQuotedAt().toInstant());
            OffsetDateTime quotedAt = brasilia(rate.getQuotedAt());
            rateView = new DashboardExchangeRateView(USD_BRL,
                    new FinancialAmount(rate.getRate()).value(), quotedAt, stale);
            if (stale) {
                warnings.add(new DashboardWarningView(
                        DashboardWarningType.STALE_EXCHANGE_RATE, null, quotedAt));
            }
        }

        BigDecimal realized = selectedBroker == null
                ? movements.sumRealizedResultByAccountIdAndType(accountId, MovementType.SALE)
                : movements.sumRealizedResultByAccountIdAndTypeAndBrokerName(
                        accountId, MovementType.SALE,
                        selectedBroker.getBroker().getTradeName());
        InvestmentResults results = PositionFinancialCalculator.consolidate(
                new FinancialAmount(account.getBalance()), new FinancialAmount(realized), valuations);
        DashboardDistributionsView distributions = new DashboardDistributionsView(
                slices(byAsset), slices(byBroker), slices(byMarket));
        return new DashboardSnapshot(results.balance().value(), true, brokerAssociationId,
                List.copyOf(positionViews),
                results.marketValue().value(), results.patrimony().value(),
                results.realizedResult().value(), results.unrealizedResult().value(),
                results.totalResult().value(), distributions, rateView, List.copyOf(warnings));
    }

    private static void accumulate(Map<String, DistributionAccumulator> distribution,
            String identifier, String label, FinancialAmount value) {
        distribution.compute(identifier, (ignored, current) -> current == null
                ? new DistributionAccumulator(label, value)
                : new DistributionAccumulator(current.label(), current.value().add(value)));
    }

    private static List<DashboardDistributionSliceView> slices(
            Map<String, DistributionAccumulator> distribution) {
        return distribution.entrySet().stream()
                .map(entry -> new DashboardDistributionSliceView(entry.getKey(),
                        entry.getValue().label(), entry.getValue().value().value()))
                .toList();
    }

    private Quote usableQuote(Position position) {
        Quote quote = quotes.findById(position.getAsset().getId())
                .orElseThrow(ExternalDependencyException::new);
        if (quote.getPrice() == null || quote.getPrice().signum() <= 0
                || quote.getCurrency() != position.getAsset().getCurrency()
                || quote.getQuotedAt() == null) {
            throw new ExternalDependencyException();
        }
        return quote;
    }

    private ExchangeRate usableExchangeRate() {
        ExchangeRate rate = exchangeRates.findById(USD_BRL)
                .orElseThrow(ExternalDependencyException::new);
        if (rate.getRate() == null || rate.getRate().signum() <= 0 || rate.getQuotedAt() == null) {
            throw new ExternalDependencyException();
        }
        return rate;
    }

    private static boolean requiresUsd(List<Position> positions) {
        return positions.stream().anyMatch(position -> position.getAsset().getCurrency() == Currency.USD);
    }

    private static OffsetDateTime brasilia(OffsetDateTime value) {
        return value.atZoneSameInstant(BRASILIA).toOffsetDateTime();
    }

    private record DistributionAccumulator(String label, FinancialAmount value) { }
}
