package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
class PurchaseServiceIntegrationTests {
    private static final Instant NOW = Instant.parse("2026-09-03T13:00:00Z");
    @Autowired private PurchaseService service;
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
    private Account otherAccount;
    private AccountBroker association;

    @BeforeEach
    void setUp() {
        cleanup();
        account = saveAccount("52998224725", "buyer@example.com", "1000.00");
        otherAccount = saveAccount("11144477735", "other@example.com", "1000.00");
        Broker broker = brokers.save(broker("02332886000104", "XP"));
        association = associations.save(AccountBroker.create(
                UUID.randomUUID(), account, broker, now()));
    }

    @AfterEach
    void cleanup() {
        points.deleteAll();
        movements.deleteAll();
        positions.deleteAll();
        quotes.deleteAll();
        rates.deleteAll();
        associations.deleteAll();
        brokers.deleteAll();
        assets.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void buysBrazilianAssetAndUpdatesWeightedAverageAtomically() {
        when(brazil.findQuote("PETR4")).thenReturn(quote("PETR4", Market.BR, Currency.BRL, "10.00"));
        var cachedAsset = new com.projeto.gestao.domain.model.Asset(
                "PETR4", "PETR4", Market.BR, Currency.BRL);
        cachedAsset = assets.save(cachedAsset);

        PurchaseResult first = service.purchase(account.getId(), cachedAsset.getId(),
                association.getId(), 10);
        when(brazil.findQuote("PETR4")).thenReturn(quote("PETR4", Market.BR, Currency.BRL, "20.00"));
        PurchaseResult second = service.purchase(account.getId(), cachedAsset.getId(),
                association.getId(), 10);

        assertThat(first.purchaseAmountBrl()).isEqualByComparingTo("100.00");
        assertThat(second.positionQuantity()).isEqualTo(20);
        assertThat(second.positionAveragePriceBrl()).isEqualByComparingTo("15.00");
        assertThat(second.positionTotalCostBrl()).isEqualByComparingTo("300.00");
        assertThat(second.remainingBalanceBrl()).isEqualByComparingTo("700.00");
        assertThat(movements.count()).isEqualTo(2);
        assertThat(points.count()).isEqualTo(2);
        assertThat(movements.findAll()).allMatch(m -> m.getMovementType() == MovementType.PURCHASE);
    }

    @Test
    void convertsUnitedStatesPurchaseToBrlWithoutFees() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "AAPL", "Apple", Market.US, Currency.USD));
        when(unitedStates.findQuote("AAPL"))
                .thenReturn(quote("AAPL", Market.US, Currency.USD, "10.00"));
        when(exchange.currentRate()).thenReturn(new UsdBrlRate(
                new BigDecimal("5.005"), NOW, NOW, "AwesomeAPI"));

        PurchaseResult result = service.purchase(account.getId(), asset.getId(), association.getId(), 2);

        assertThat(result.originalUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(result.unitPriceBrl()).isEqualByComparingTo("50.10");
        assertThat(result.purchaseAmountBrl()).isEqualByComparingTo("100.20");
        assertThat(result.usdBrlRate()).isEqualByComparingTo("5.01");
        var movement = movements.findAll().get(0);
        assertThat(movement.getQuotePrice()).isEqualByComparingTo("10.00");
        assertThat(movement.getUnitPriceBrl()).isEqualByComparingTo("50.10");
        assertThat(movement.getUsdBrlRate()).isEqualByComparingTo("5.01");
        assertThat(movement.getTotalAmount()).isEqualByComparingTo("100.20");
        assertThat(points.findAll().get(0).getUsdBrlRate()).isEqualByComparingTo("5.01");
    }

    @Test
    void rejectsInsufficientBalanceWithoutAnyPartialRecord() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "VALE3", "Vale", Market.BR, Currency.BRL));
        when(brazil.findQuote("VALE3")).thenReturn(quote("VALE3", Market.BR, Currency.BRL, "600.00"));

        assertThatThrownBy(() -> service.purchase(account.getId(), asset.getId(), association.getId(), 2))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(positions.count()).isZero();
        assertThat(movements.count()).isZero();
        assertThat(points.count()).isZero();
    }

    @Test
    void rejectsAssociationFromAnotherAccountWithoutRevealingIt() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "ITUB4", "Itau", Market.BR, Currency.BRL));
        when(brazil.findQuote("ITUB4")).thenReturn(quote("ITUB4", Market.BR, Currency.BRL, "30.00"));

        assertThatThrownBy(() -> service.purchase(otherAccount.getId(), asset.getId(),
                association.getId(), 1)).isInstanceOf(AuthorizationException.class);
        assertThat(quotes.count()).isZero();
        assertThat(movements.count()).isZero();
    }

    @Test
    void blocksPurchaseWhenNoUsableMarketValueExists() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "WEGE3", "Weg", Market.BR, Currency.BRL));
        when(brazil.findQuote("WEGE3")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TRANSPORT_ERROR, "provider", "offline"));

        assertThatThrownBy(() -> service.purchase(account.getId(), asset.getId(), association.getId(), 1))
                .isInstanceOf(ExternalDependencyException.class);
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(movements.count()).isZero();
    }

    @Test
    void usesPersistedQuoteFallbackAndPreservesItsOriginalInstant() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "ABEV3", "Ambev", Market.BR, Currency.BRL));
        when(brazil.findQuote("ABEV3")).thenReturn(quote("ABEV3", Market.BR, Currency.BRL, "15.00"));
        PurchaseResult first = service.purchase(account.getId(), asset.getId(), association.getId(), 1);
        when(brazil.findQuote("ABEV3")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "provider", "timeout"));

        PurchaseResult fallback = service.purchase(account.getId(), asset.getId(), association.getId(), 1);

        assertThat(fallback.originalUnitPrice()).isEqualByComparingTo("15.00");
        org.assertj.core.api.Assertions.assertThat(fallback.quoteQuotedAt()).isEqualTo(first.quoteQuotedAt());
        assertThat(movements.count()).isEqualTo(2);
    }

    @Test
    void blocksUnitedStatesPurchaseWhenExchangeRateIsUnavailable() {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "MSFT", "Microsoft", Market.US, Currency.USD));
        when(unitedStates.findQuote("MSFT"))
                .thenReturn(quote("MSFT", Market.US, Currency.USD, "10.00"));
        when(exchange.currentRate()).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "exchange", "timeout"));

        assertThatThrownBy(() -> service.purchase(account.getId(), asset.getId(), association.getId(), 1))
                .isInstanceOf(ExternalDependencyException.class);
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(positions.count()).isZero();
        assertThat(movements.count()).isZero();
        assertThat(points.count()).isZero();
    }

    @Test
    void serializesConcurrentPurchasesSharingTheAccountBalance() throws Exception {
        var asset = assets.save(new com.projeto.gestao.domain.model.Asset(
                "BBDC4", "Bradesco", Market.BR, Currency.BRL));
        when(brazil.findQuote("BBDC4")).thenReturn(quote("BBDC4", Market.BR, Currency.BRL, "600.00"));
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<Boolean> first = buyAfter(start, asset.getId());
        CompletableFuture<Boolean> second = buyAfter(start, asset.getId());

        start.countDown();
        CompletableFuture.allOf(first, second).get(10, TimeUnit.SECONDS);

        assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("400.00");
        assertThat(positions.findAll()).singleElement().satisfies(
                position -> assertThat(position.getQuantity()).isEqualTo(1));
        assertThat(movements.count()).isEqualTo(1);
        assertThat(points.count()).isEqualTo(1);
    }

    private CompletableFuture<Boolean> buyAfter(CountDownLatch start, UUID assetId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
                service.purchase(account.getId(), assetId, association.getId(), 1);
                return true;
            } catch (BusinessRuleException exception) {
                return false;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", exception);
            }
        });
    }

    private Account saveAccount(String cpf, String email, String balance) {
        return accounts.save(Account.create(UUID.randomUUID(), "Investor", cpf, email, "hash",
                new BigDecimal(balance), now()));
    }

    private static Broker broker(String cnpj, String name) {
        return Broker.create(UUID.randomUUID(), cnpj, name + " SA", name, "ATIVA", "CTVM",
                "01001000", "Rua A", "1", null, "Centro", "São Paulo", "SP", now());
    }

    private static MarketQuote quote(String ticker, Market market, Currency currency, String price) {
        return new MarketQuote(ticker, ticker, market, currency, new BigDecimal(price),
                NOW.plusSeconds(new BigDecimal(price).intValue()), NOW, "provider");
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.parse("2026-09-03T10:00:00-03:00");
    }
}
