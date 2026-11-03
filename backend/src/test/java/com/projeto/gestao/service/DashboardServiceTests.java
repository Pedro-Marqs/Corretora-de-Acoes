package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.AuthorizationException;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.domain.model.PatrimonialPoint;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.PositionBalance;
import com.projeto.gestao.domain.model.PositionQuantity;
import com.projeto.gestao.domain.model.Quote;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTests {
    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BROKER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-27T12:00:00Z");

    @Mock AccountRepository accounts;
    @Mock AccountBrokerRepository accountBrokers;
    @Mock PositionRepository positions;
    @Mock MovementRepository movements;
    @Mock QuoteRepository quotes;
    @Mock ExchangeRateRepository exchangeRates;
    @Mock PatrimonialPointRepository patrimonialPoints;
    @Mock MarketDataFreshness freshness;

    private Account account;
    private AccountBroker association;
    private DashboardService dashboard;

    @BeforeEach
    void setUp() {
        account = Account.create(ACCOUNT_ID, "Investor", "12345678901",
                "investor@example.com", "hash", amount("1000"), NOW.minusMonths(6));
        association = association("Primary");
        dashboard = new DashboardService(accounts, accountBrokers, positions, movements,
                quotes, exchangeRates, patrimonialPoints, freshness,
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));

        when(accounts.findByIdAndStatus(ACCOUNT_ID, AccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        lenient().when(positions.findByAccountIdAndQuantityGreaterThan(ACCOUNT_ID, 0L))
                .thenReturn(List.of());
        lenient().when(movements.sumRealizedResultByAccountIdAndType(ACCOUNT_ID, MovementType.SALE))
                .thenReturn(amount("0"));
        lenient().when(movements.sumRealizedResultByAccountIdAndTypeAndBrokerName(
                eq(ACCOUNT_ID), eq(MovementType.SALE), any(String.class)))
                .thenReturn(amount("0"));
        lenient().when(movements.findByAccountIdAndOccurredAtLessThanEqualOrderByOccurredAtAscIdAsc(
                eq(ACCOUNT_ID), any())).thenReturn(List.of());
        lenient().when(patrimonialPoints.findByAccountIdAndRecordedAtBetweenOrderByRecordedAtAscIdAsc(
                eq(ACCOUNT_ID), any(), any())).thenReturn(List.of());
    }

    @Test
    void rejectsMissingOrUnknownAccountAndForeignBroker() {
        assertThatThrownBy(() -> dashboard.query(null))
                .isInstanceOf(AuthenticationException.class);

        when(accounts.findByIdAndStatus(ACCOUNT_ID, AccountStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> dashboard.query(ACCOUNT_ID))
                .isInstanceOf(AuthenticationException.class);

        when(accounts.findByIdAndStatus(ACCOUNT_ID, AccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(accountBrokers.findByIdAndAccountIdAndStatus(
                BROKER_ID, ACCOUNT_ID, com.projeto.gestao.domain.model.AssociationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> dashboard.query(ACCOUNT_ID, BROKER_ID))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void returnsSharedEmptySnapshotWhenNoPeriodOrPositionsAreSelected() {
        DashboardSnapshot snapshot = dashboard.query(ACCOUNT_ID);

        assertThat(snapshot.balanceShared()).isTrue();
        assertThat(snapshot.availableBalanceBrl()).isEqualByComparingTo("1000.00");
        assertThat(snapshot.positions()).isEmpty();
        assertThat(snapshot.exchangeRate()).isNull();
        assertThat(snapshot.period()).isNull();
        assertThat(snapshot.patrimonyHistory()).isEmpty();
        assertThat(snapshot.investmentHistory()).isEmpty();
    }

    @Test
    void calculatesBrlPositionWithStaleQuoteAndPatrimonialHistoryForSelectedBroker() {
        Asset asset = new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL);
        Position position = position(asset, 10, "20", "25");
        Quote quote = new Quote(asset, amount("25"), Currency.BRL, NOW, NOW, "cache");
        quote.markStale();
        when(accountBrokers.findByIdAndAccountIdAndStatus(
                BROKER_ID, ACCOUNT_ID, com.projeto.gestao.domain.model.AssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));
        when(positions.findByAccountIdAndQuantityGreaterThan(ACCOUNT_ID, 0L))
                .thenReturn(List.of(position));
        when(quotes.findById(asset.getId())).thenReturn(Optional.of(quote));
        when(movements.sumRealizedResultByAccountIdAndTypeAndBrokerName(
                ACCOUNT_ID, MovementType.SALE, "Primary")).thenReturn(amount("10"));
        PatrimonialPoint point = mock(PatrimonialPoint.class);
        when(point.getRecordedAt()).thenReturn(NOW.minusDays(1));
        when(point.getPatrimonyBrl()).thenReturn(amount("1200"));
        when(patrimonialPoints.findByAccountIdAndRecordedAtBetweenOrderByRecordedAtAscIdAsc(
                eq(ACCOUNT_ID), any(), any())).thenReturn(List.of(point));

        DashboardSnapshot snapshot = dashboard.query(ACCOUNT_ID, BROKER_ID, DashboardPeriod.FOUR_WEEKS);

        assertThat(snapshot.positions()).hasSize(1);
        assertThat(snapshot.positions().get(0).marketValueBrl()).isEqualByComparingTo("250.00");
        assertThat(snapshot.positions().get(0).quoteStale()).isTrue();
        assertThat(snapshot.realizedResultBrl()).isEqualByComparingTo("10.00");
        assertThat(snapshot.patrimonyHistory()).hasSize(1);
        assertThat(snapshot.warnings()).hasSize(1);
        assertThat(snapshot.warnings().get(0).type()).isEqualTo(DashboardWarningType.STALE_QUOTE);
    }

    @Test
    void calculatesUsdPositionAndMarksStaleExchangeRate() {
        Asset asset = new Asset("AAPL", "Apple", Market.US, Currency.USD);
        Position position = position(asset, 2, "200", "40");
        Quote quote = new Quote(asset, amount("40"), Currency.USD, NOW, NOW, "cache");
        ExchangeRate rate = new ExchangeRate("USD/BRL", amount("5"), NOW, NOW, "cache");
        rate.markStale();
        when(positions.findByAccountIdAndQuantityGreaterThan(ACCOUNT_ID, 0L))
                .thenReturn(List.of(position));
        when(quotes.findById(asset.getId())).thenReturn(Optional.of(quote));
        when(exchangeRates.findById("USD/BRL")).thenReturn(Optional.of(rate));

        DashboardSnapshot snapshot = dashboard.query(ACCOUNT_ID, null, DashboardPeriod.THREE_MONTHS);

        assertThat(snapshot.positions().get(0).marketValueBrl()).isEqualByComparingTo("400.00");
        assertThat(snapshot.exchangeRate().rate()).isEqualByComparingTo("5.00");
        assertThat(snapshot.exchangeRate().stale()).isTrue();
        assertThat(snapshot.warnings()).extracting(DashboardWarningView::type)
                .containsExactly(DashboardWarningType.STALE_EXCHANGE_RATE);
    }

    @Test
    void rejectsMissingOrInvalidQuoteAndExchangeRate() {
        Asset brlAsset = new Asset("VALE3", "Vale", Market.BR, Currency.BRL);
        when(positions.findByAccountIdAndQuantityGreaterThan(ACCOUNT_ID, 0L))
                .thenReturn(List.of(position(brlAsset, 1, "20", "20")));
        when(quotes.findById(brlAsset.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboard.query(ACCOUNT_ID))
                .isInstanceOf(ExternalDependencyException.class);

        Asset usdAsset = new Asset("MSFT", "Microsoft", Market.US, Currency.USD);
        when(positions.findByAccountIdAndQuantityGreaterThan(ACCOUNT_ID, 0L))
                .thenReturn(List.of(position(usdAsset, 1, "100", "40")));
        when(exchangeRates.findById("USD/BRL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboard.query(ACCOUNT_ID))
                .isInstanceOf(ExternalDependencyException.class);
    }

    @Test
    void createsInvestmentHistoryWithPurchasesSalesAndBrokerTransfers() {
        when(accountBrokers.findByIdAndAccountIdAndStatus(
                BROKER_ID, ACCOUNT_ID, com.projeto.gestao.domain.model.AssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));
        OffsetDateTime beforePeriod = NOW.minusMonths(2);
        Movement oldPurchase = Movement.purchase(UUID.randomUUID(), account, "PETR4", Market.BR,
                amount("10"), amount("10"), null, 2, amount("20"), Currency.BRL,
                "Primary", amount("980"), beforePeriod);
        Movement purchase = Movement.purchase(UUID.randomUUID(), account, "PETR4", Market.BR,
                amount("20"), amount("20"), null, 10, amount("200"), Currency.BRL,
                "Primary", amount("800"), NOW.minusDays(10));
        Movement sale = Movement.sale(UUID.randomUUID(), account, "PETR4", Market.BR,
                amount("25"), amount("25"), null, 3, amount("75"), Currency.BRL,
                "Primary", amount("875"), amount("5"), NOW.minusDays(9));
        Movement transferIn = Movement.transfer(UUID.randomUUID(), account, "PETR4", Market.BR,
                2, amount("50"), Currency.BRL, "Other", "Primary", amount("875"),
                NOW.minusDays(8));
        Movement transferOut = Movement.transfer(UUID.randomUUID(), account, "PETR4", Market.BR,
                1, amount("25"), Currency.BRL, "Primary", "Other", amount("875"),
                NOW.minusDays(7));
        Movement unrelatedTransfer = Movement.transfer(UUID.randomUUID(), account, "PETR4", Market.BR,
                1, amount("25"), Currency.BRL, "Other A", "Other B", amount("875"),
                NOW.minusDays(6));
        Movement deposit = Movement.deposit(UUID.randomUUID(), account, amount("100"),
                amount("975"), NOW.minusDays(5));
        when(movements.findByAccountIdAndOccurredAtLessThanEqualOrderByOccurredAtAscIdAsc(
                eq(ACCOUNT_ID), any())).thenReturn(List.of(oldPurchase, purchase, sale,
                        transferIn, transferOut, unrelatedTransfer, deposit));

        DashboardSnapshot snapshot = dashboard.query(ACCOUNT_ID, BROKER_ID, DashboardPeriod.FOUR_WEEKS);

        assertThat(snapshot.investmentHistory()).hasSize(6);
        assertThat(snapshot.investmentHistory().get(0).positionsValueBrl())
                .isEqualByComparingTo("240.00");
        assertThat(snapshot.investmentHistory().get(4).positionsValueBrl())
                .isEqualByComparingTo("250.00");
    }

    @Test
    void returnsAllInvestmentPointsWhenNoMovementBelongsToSelectedBroker() {
        when(accountBrokers.findByIdAndAccountIdAndStatus(
                BROKER_ID, ACCOUNT_ID, com.projeto.gestao.domain.model.AssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));
        Movement ignoredPurchase = Movement.purchase(UUID.randomUUID(), account, "PETR4", Market.BR,
                amount("20"), amount("20"), null, 1, amount("20"), Currency.BRL,
                "Other", amount("980"), NOW.minusDays(2));
        Movement ignoredDeposit = Movement.deposit(UUID.randomUUID(), account, amount("10"),
                amount("990"), NOW.minusDays(1));
        when(movements.findByAccountIdAndOccurredAtLessThanEqualOrderByOccurredAtAscIdAsc(
                eq(ACCOUNT_ID), any())).thenReturn(List.of(ignoredPurchase, ignoredDeposit));

        DashboardSnapshot snapshot = dashboard.query(ACCOUNT_ID, BROKER_ID, DashboardPeriod.FOUR_WEEKS);

        assertThat(snapshot.investmentHistory()).hasSize(2);
        assertThat(snapshot.investmentHistory().get(0).positionsValueBrl()).isZero();
        assertThat(snapshot.investmentHistory().get(1).positionsValueBrl()).isZero();
    }

    private AccountBroker association(String tradeName) {
        Broker broker = Broker.create(BROKER_ID, "12345678000199", tradeName + " SA", tradeName,
                "ATIVA", "CTVM", "01001000", "Rua A", "1", null, "Centro",
                "Sao Paulo", "SP", NOW);
        return AccountBroker.create(BROKER_ID, account, broker, NOW);
    }

    private Position position(Asset asset, long quantity, String averagePrice, String quotePrice) {
        return Position.create(UUID.randomUUID(), account, association, asset,
                new PositionBalance(PositionQuantity.positive(quantity),
                        new com.projeto.gestao.domain.model.FinancialAmount(amount(averagePrice))
                                .multiply(quantity),
                        new com.projeto.gestao.domain.model.FinancialAmount(amount(averagePrice))));
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
