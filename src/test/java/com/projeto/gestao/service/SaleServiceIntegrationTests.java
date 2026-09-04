package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.projeto.gestao.api.exception.AuthorizationException;
import com.projeto.gestao.api.exception.BusinessRuleException;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.domain.model.UsdBrlRate;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsMarketDataPort;
import com.projeto.gestao.domain.port.UsdBrlExchangeRatePort;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AssetRepository;
import com.projeto.gestao.repository.BrokerRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SaleServiceIntegrationTests {
    private static final Instant FIRST = Instant.parse("2026-09-03T13:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-09-03T13:05:00Z");
    @Autowired private PurchaseService purchases;
    @Autowired private SaleService sales;
    @Autowired private AccountRepository accounts;
    @Autowired private BrokerRepository brokers;
    @Autowired private AccountBrokerRepository associations;
    @Autowired private AssetRepository assets;
    @Autowired private QuoteRepository quotes;
    @Autowired private ExchangeRateRepository rates;
    @Autowired private PositionRepository positions;
    @Autowired private MovementRepository movements;
    @Autowired private PatrimonialPointRepository points;
    @MockitoBean private BrazilMarketDataPort brazil;
    @MockitoBean private UsMarketDataPort unitedStates;
    @MockitoBean private UsdBrlExchangeRatePort exchange;
    private Account account;
    private Account other;
    private AccountBroker association;

    @BeforeEach
    void setUp() {
        cleanup();
        account = saveAccount("52998224725", "seller@example.com");
        other = saveAccount("11144477735", "other-seller@example.com");
        Broker broker = brokers.save(Broker.create(UUID.randomUUID(), "02332886000104", "XP SA", "XP",
                "ATIVA", "CTVM", "01001000", "Rua A", "1", null, "Centro", "São Paulo", "SP", now()));
        association = associations.save(AccountBroker.create(UUID.randomUUID(), account, broker, now()));
    }

    @AfterEach
    void cleanup() {
        points.deleteAll(); movements.deleteAll(); positions.deleteAll(); quotes.deleteAll();
        rates.deleteAll(); associations.deleteAll(); brokers.deleteAll(); assets.deleteAll(); accounts.deleteAll();
    }

    @Test
    void sellsPartiallyAndTotallyPreservingAverageAndHistoryThenAllowsRepurchase() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "PETR4", "Petrobras", Market.BR, Currency.BRL));
        when(brazil.findQuote("PETR4")).thenReturn(quote("PETR4", "25.00", FIRST),
                quote("PETR4", "30.00", SECOND), quote("PETR4", "20.00", SECOND.plusSeconds(300)));
        purchases.purchase(account.getId(), asset.getId(), association.getId(), 20);

        SaleResult partial = sales.sell(account.getId(), asset.getId(), association.getId(), 5);
        assertThat(partial.positionQuantity()).isEqualTo(15);
        assertThat(partial.positionAveragePriceBrl()).isEqualByComparingTo("25.00");
        assertThat(partial.positionTotalCostBrl()).isEqualByComparingTo("375.00");
        assertThat(partial.realizedResultBrl()).isEqualByComparingTo("25.00");
        assertThat(partial.remainingBalanceBrl()).isEqualByComparingTo("650.00");

        SaleResult total = sales.sell(account.getId(), asset.getId(), association.getId(), 15);
        assertThat(total.positionQuantity()).isZero();
        assertThat(total.positionAveragePriceBrl()).isEqualByComparingTo("0.00");
        assertThat(total.positionTotalCostBrl()).isEqualByComparingTo("0.00");
        assertThat(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0)).isEmpty();
        assertThat(movements.findAll()).extracting(m -> m.getMovementType())
                .containsExactlyInAnyOrder(MovementType.PURCHASE, MovementType.SALE, MovementType.SALE);

        purchases.purchase(account.getId(), asset.getId(), association.getId(), 2);
        var repurchased = positions.findByAccountIdAndAccountBrokerIdAndAssetId(
                account.getId(), association.getId(), asset.getId()).orElseThrow();
        assertThat(repurchased.getAveragePrice()).isEqualByComparingTo("20.00");
        assertThat(points.count()).isEqualTo(4);
    }

    @Test
    void convertsUsSaleAndPersistsEffectiveFinancialInputsAndNegativeResult() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "AAPL", "Apple", Market.US, Currency.USD));
        when(unitedStates.findQuote("AAPL")).thenReturn(quoteUs("AAPL", "10.00", FIRST));
        when(exchange.currentRate()).thenReturn(new UsdBrlRate(new BigDecimal("5.00"), FIRST,
                FIRST.plusSeconds(1), "AwesomeAPI"));
        purchases.purchase(account.getId(), asset.getId(), association.getId(), 3);
        // US lookup is cache-first; replace the stored daily snapshot to represent the next refresh.
        var storedQuote = quotes.findById(asset.getId()).orElseThrow();
        storedQuote.replace(new BigDecimal("8.00"), Currency.USD,
                SECOND.atOffset(java.time.ZoneOffset.UTC),
                SECOND.plusSeconds(1).atOffset(java.time.ZoneOffset.UTC), "TwelveData");
        quotes.saveAndFlush(storedQuote);

        SaleResult result = sales.sell(account.getId(), asset.getId(), association.getId(), 2);

        assertThat(result.saleAmountBrl()).isEqualByComparingTo("80.00");
        assertThat(result.realizedResultBrl()).isEqualByComparingTo("-20.00");
        var movement = movements.findAll().stream()
                .filter(m -> m.getMovementType() == MovementType.SALE).findFirst().orElseThrow();
        assertThat(movement.getQuotePrice()).isEqualByComparingTo("8.00");
        assertThat(movement.getUnitPriceBrl()).isEqualByComparingTo("40.00");
        assertThat(movement.getUsdBrlRate()).isEqualByComparingTo("5.00");
        assertThat(movement.getRealizedResult()).isEqualByComparingTo("-20.00");
    }

    @Test
    void rejectsInsufficientOrForeignPositionWithoutAnySaleRecord() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "PETR4", "Petrobras", Market.BR, Currency.BRL));
        when(brazil.findQuote("PETR4")).thenReturn(quote("PETR4", "25.00", FIRST));
        purchases.purchase(account.getId(), asset.getId(), association.getId(), 2);
        long movementCount = movements.count();
        long pointCount = points.count();

        assertThatThrownBy(() -> sales.sell(account.getId(), asset.getId(), association.getId(), 3))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> sales.sell(other.getId(), asset.getId(), association.getId(), 1))
                .isInstanceOf(AuthorizationException.class);
        assertThat(movements.count()).isEqualTo(movementCount);
        assertThat(points.count()).isEqualTo(pointCount);
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("950.00");
    }

    @Test
    void usesPersistedQuoteFallbackAndPreservesItsOriginalInstant() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "ABEV3", "Ambev", Market.BR, Currency.BRL));
        when(brazil.findQuote("ABEV3")).thenReturn(quote("ABEV3", "15.00", FIRST));
        purchases.purchase(account.getId(), asset.getId(), association.getId(), 2);
        Instant storedInstant = quotes.findById(asset.getId()).orElseThrow()
                .getQuotedAt().toInstant();
        when(brazil.findQuote("ABEV3")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "provider", "timeout"));

        SaleResult result = sales.sell(account.getId(), asset.getId(), association.getId(), 1);

        assertThat(result.originalUnitPrice()).isEqualByComparingTo("15.00");
        assertThat(result.quoteQuotedAt()).isEqualTo(storedInstant);
        assertThat(movements.findAll().stream()
                .filter(movement -> movement.getMovementType() == MovementType.SALE)).hasSize(1);
    }

    @Test
    void blocksSaleWhenNoUsableQuoteExistsWithoutChangingFinancialState() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "WEGE3", "Weg", Market.BR, Currency.BRL));
        when(brazil.findQuote("WEGE3")).thenReturn(quote("WEGE3", "20.00", FIRST));
        purchases.purchase(account.getId(), asset.getId(), association.getId(), 2);
        quotes.deleteAll();
        when(brazil.findQuote("WEGE3")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TRANSPORT_ERROR, "provider", "offline"));
        long movementCount = movements.count();
        long pointCount = points.count();

        assertThatThrownBy(() -> sales.sell(
                account.getId(), asset.getId(), association.getId(), 1))
                .isInstanceOf(ExternalDependencyException.class);
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("960.00");
        assertThat(positions.findByAccountIdAndAccountBrokerIdAndAssetId(
                account.getId(), association.getId(), asset.getId()).orElseThrow().getQuantity())
                .isEqualTo(2);
        assertThat(movements.count()).isEqualTo(movementCount);
        assertThat(points.count()).isEqualTo(pointCount);
    }

    @Test
    void blocksUnitedStatesSaleWhenExchangeRateIsUnavailable() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "MSFT", "Microsoft", Market.US, Currency.USD));
        when(unitedStates.findQuote("MSFT")).thenReturn(quoteUs("MSFT", "10.00", FIRST));
        when(exchange.currentRate()).thenReturn(new UsdBrlRate(
                new BigDecimal("5.00"), FIRST, FIRST.plusSeconds(1), "AwesomeAPI"));
        purchases.purchase(account.getId(), asset.getId(), association.getId(), 2);
        rates.deleteAll();
        when(exchange.currentRate()).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "exchange", "timeout"));
        long movementCount = movements.count();
        long pointCount = points.count();

        assertThatThrownBy(() -> sales.sell(
                account.getId(), asset.getId(), association.getId(), 1))
                .isInstanceOf(ExternalDependencyException.class);
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("900.00");
        assertThat(positions.findByAccountIdAndAccountBrokerIdAndAssetId(
                account.getId(), association.getId(), asset.getId()).orElseThrow().getQuantity())
                .isEqualTo(2);
        assertThat(movements.count()).isEqualTo(movementCount);
        assertThat(points.count()).isEqualTo(pointCount);
    }

    @Test
    void serializesConcurrentSalesSoQuantityCannotBeOversold() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "PETR4", "Petrobras", Market.BR, Currency.BRL));
        when(brazil.findQuote("PETR4")).thenReturn(quote("PETR4", "25.00", FIRST));
        purchases.purchase(account.getId(), asset.getId(), association.getId(), 5);
        CompletableFuture<Boolean> first = sellAsync(asset.getId(), 3);
        CompletableFuture<Boolean> second = sellAsync(asset.getId(), 3);

        assertThat(java.util.List.of(first.join(), second.join())).containsExactlyInAnyOrder(true, false);
        assertThat(positions.findByAccountIdAndAccountBrokerIdAndAssetId(
                account.getId(), association.getId(), asset.getId()).orElseThrow().getQuantity()).isEqualTo(2);
        assertThat(movements.findAll().stream().filter(m -> m.getMovementType() == MovementType.SALE)).hasSize(1);
    }

    private CompletableFuture<Boolean> sellAsync(UUID assetId, long quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try { sales.sell(account.getId(), assetId, association.getId(), quantity); return true; }
            catch (BusinessRuleException exception) { return false; }
        });
    }

    private Account saveAccount(String cpf, String email) {
        return accounts.save(Account.create(UUID.randomUUID(), "Investor", cpf, email, "hash",
                new BigDecimal("1000.00"), now()));
    }

    private static MarketQuote quote(String ticker, String price, Instant instant) {
        return new MarketQuote(ticker, "Asset", Market.BR, Currency.BRL, new BigDecimal(price),
                instant, instant.plusSeconds(1), "Brapi");
    }

    private static MarketQuote quoteUs(String ticker, String price, Instant instant) {
        return new MarketQuote(ticker, "Asset", Market.US, Currency.USD, new BigDecimal(price),
                instant, instant.plusSeconds(1), "TwelveData");
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.parse("2026-09-03T10:00:00-03:00");
    }
}
