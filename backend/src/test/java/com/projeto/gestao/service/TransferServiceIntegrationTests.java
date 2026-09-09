package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.projeto.gestao.api.exception.AuthorizationException;
import com.projeto.gestao.api.exception.BusinessRuleException;
import com.projeto.gestao.api.exception.MarketDataUnavailableException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.domain.model.CompanyRegistration;
import com.projeto.gestao.domain.model.RegulatoryRegistration;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
import com.projeto.gestao.domain.port.CompanyRegistryPort;
import com.projeto.gestao.domain.port.RegulatoryRegistryPort;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AssetRepository;
import com.projeto.gestao.repository.BrokerRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class TransferServiceIntegrationTests {
    private static final Instant INSTANT = Instant.parse("2026-09-03T13:00:00Z");

    @Autowired private TransferService transfers;
    @Autowired private PurchaseService purchases;
    @Autowired private SaleService sales;
    @Autowired private AccountRepository accounts;
    @Autowired private BrokerRepository brokers;
    @Autowired private AccountBrokerRepository associations;
    @Autowired private AssetRepository assets;
    @Autowired private QuoteRepository quotes;
    @Autowired private PositionRepository positions;
    @Autowired private MovementRepository movements;
    @Autowired private PatrimonialPointRepository points;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private BrazilMarketDataPort brazil;
    @MockitoBean private CompanyRegistryPort companies;
    @MockitoBean private RegulatoryRegistryPort regulatoryRegistry;

    private Account account;
    private Account other;
    private AccountBroker origin;
    private AccountBroker destination;
    private AccountBroker alternate;
    private AccountBroker foreign;
    private Asset asset;

    @BeforeEach
    void setUp() {
        cleanup();
        account = saveAccount("52998224725", "transfer@example.com");
        other = saveAccount("11144477735", "other-transfer@example.com");
        origin = association(account, "02332886000104", "Origem");
        destination = association(account, "10270580000110", "Destino");
        alternate = association(account, "34028316000103", "Alternativa");
        foreign = association(other, "60872504000123", "Terceira");
        asset = assets.save(new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL));
        when(companies.findByCnpj(org.mockito.ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> new CompanyRegistration(invocation.getArgument(0),
                "Corretora", "Corretora", "ATIVA", "01001000"));
        when(regulatoryRegistry.findByCnpj(org.mockito.ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> new RegulatoryRegistration(invocation.getArgument(0),
                true, true, List.of()));
    }

    @AfterEach
    void cleanup() {
        points.deleteAll(); movements.deleteAll(); positions.deleteAll(); quotes.deleteAll();
        associations.deleteAll(); brokers.deleteAll(); assets.deleteAll(); accounts.deleteAll();
    }

    @Test
    void persistsEmptyExistingPartialAndTotalTransfersConservingCostAndAverage() {
        when(brazil.findQuote("PETR4")).thenReturn(quote("10.00"), quote("20.00", 1));
        purchases.purchase(account.getId(), asset.getId(), origin.getId(), 4);

        TransferResult emptyDestination = transfers.transfer(account.getId(), origin.getId(),
                destination.getId(), asset.getId(), 2);
        assertThat(emptyDestination.originQuantity()).isEqualTo(2);
        assertThat(emptyDestination.originTotalCostBrl()).isEqualByComparingTo("20.00");
        assertThat(emptyDestination.destinationQuantity()).isEqualTo(2);
        assertThat(emptyDestination.destinationAveragePriceBrl()).isEqualByComparingTo("10.00");
        assertThat(emptyDestination.transferredCostBrl()).isEqualByComparingTo("20.00");

        purchases.purchase(account.getId(), asset.getId(), destination.getId(), 2);
        TransferResult total = transfers.transfer(account.getId(), origin.getId(),
                destination.getId(), asset.getId(), 2);

        assertThat(total.originQuantity()).isZero();
        assertThat(total.originAveragePriceBrl()).isEqualByComparingTo("0.00");
        assertThat(total.originTotalCostBrl()).isEqualByComparingTo("0.00");
        assertThat(total.destinationQuantity()).isEqualTo(6);
        assertThat(total.destinationTotalCostBrl()).isEqualByComparingTo("80.00");
        assertThat(total.destinationAveragePriceBrl()).isEqualByComparingTo("13.33");
        assertThat(total.remainingBalanceBrl()).isEqualByComparingTo("920.00");
        assertThat(position(origin).getQuantity()).isZero();
        assertThat(movements.findAll()).extracting(item -> item.getMovementType())
                .containsExactlyInAnyOrder(MovementType.PURCHASE, MovementType.PURCHASE,
                        MovementType.TRANSFER, MovementType.TRANSFER);
        var transferMovements = movements.findAll().stream()
                .filter(item -> item.getMovementType() == MovementType.TRANSFER).toList();
        assertThat(transferMovements).allSatisfy(item -> assertThat(item.getRealizedResult()).isNull());
        assertThat(transferMovements).extracting(item -> item.getRemainingBalance())
                .containsExactlyInAnyOrder(new BigDecimal("960.00"), new BigDecimal("920.00"));
        assertThat(points.count()).isEqualTo(4);
    }

    @Test
    void rejectsForeignInactiveSameMissingAndInsufficientResourcesWithoutChanges() {
        when(brazil.findQuote("PETR4")).thenReturn(quote("10.00"));
        purchases.purchase(account.getId(), asset.getId(), origin.getId(), 3);
        long movementCount = movements.count();
        long pointCount = points.count();

        assertThatThrownBy(() -> transfers.transfer(account.getId(), origin.getId(),
                foreign.getId(), asset.getId(), 1)).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> transfers.transfer(account.getId(), origin.getId(),
                origin.getId(), asset.getId(), 1)).isInstanceOf(AuthorizationException.class);
        destination.inactivate(now());
        associations.saveAndFlush(destination);
        assertThatThrownBy(() -> transfers.transfer(account.getId(), origin.getId(),
                destination.getId(), asset.getId(), 1)).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> transfers.transfer(account.getId(), origin.getId(),
                alternate.getId(), asset.getId(), 4)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> transfers.transfer(account.getId(), origin.getId(),
                alternate.getId(), UUID.randomUUID(), 1))
                .isInstanceOf(MarketDataUnavailableException.class);
        jdbc.update("UPDATE asset SET status = 'INACTIVE' WHERE id = ?", asset.getId());
        assertThatThrownBy(() -> transfers.transfer(account.getId(), origin.getId(),
                alternate.getId(), asset.getId(), 1))
                .isInstanceOf(MarketDataUnavailableException.class);

        assertThat(position(origin).getQuantity()).isEqualTo(3);
        assertThat(position(origin).getTotalCost()).isEqualByComparingTo("30.00");
        assertThat(positions.findByAccountIdAndAccountBrokerIdAndAssetId(account.getId(),
                alternate.getId(), asset.getId())).isEmpty();
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("970.00");
        assertThat(movements.count()).isEqualTo(movementCount);
        assertThat(points.count()).isEqualTo(pointCount);
    }

    @Test
    void serializesOverlappingTransfersWithoutDuplicateConsumptionOrDeadlock() {
        when(brazil.findQuote("PETR4")).thenReturn(quote("10.00"));
        purchases.purchase(account.getId(), asset.getId(), origin.getId(), 5);
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<Boolean> first = transferAfter(start, destination, 3);
        CompletableFuture<Boolean> second = transferAfter(start, alternate, 3);
        start.countDown();

        assertThat(List.of(first.join(), second.join())).containsExactlyInAnyOrder(true, false);
        List<com.projeto.gestao.domain.model.Position> open =
                positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0);
        assertThat(open.stream().mapToLong(item -> item.getQuantity()).sum()).isEqualTo(5);
        assertThat(open.stream().map(item -> item.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("50.00");
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("950.00");
        assertThat(movements.findAll().stream()
                .filter(item -> item.getMovementType() == MovementType.TRANSFER)).hasSize(1);
    }

    @Test
    void coordinatesTransferWithConcurrentPurchaseAndPreservesBothCosts() {
        when(brazil.findQuote("PETR4")).thenReturn(quote("10.00"), quote("30.00", 1));
        purchases.purchase(account.getId(), asset.getId(), origin.getId(), 4);
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<Void> transfer = CompletableFuture.runAsync(() -> {
            await(start);
            transfers.transfer(account.getId(), origin.getId(), destination.getId(), asset.getId(), 2);
        });
        CompletableFuture<Void> purchase = CompletableFuture.runAsync(() -> {
            await(start);
            purchases.purchase(account.getId(), asset.getId(), destination.getId(), 2);
        });
        start.countDown();
        CompletableFuture.allOf(transfer, purchase).join();

        assertThat(position(origin).getQuantity()).isEqualTo(2);
        var destinationPosition = position(destination);
        assertThat(destinationPosition.getQuantity()).isEqualTo(4);
        assertThat(destinationPosition.getTotalCost()).isEqualByComparingTo("80.00");
        assertThat(destinationPosition.getAveragePrice()).isEqualByComparingTo("20.00");
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("900.00");
    }

    @Test
    void serializesConcurrentSaleAndTotalTransferWithoutDeadlock() throws Exception {
        when(brazil.findQuote("PETR4")).thenReturn(quote("10.00"), quote("30.00"));
        purchases.purchase(account.getId(), asset.getId(), origin.getId(), 4);
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<Boolean> transfer = CompletableFuture.supplyAsync(() -> {
            await(start);
            try {
                transfers.transfer(account.getId(), origin.getId(), destination.getId(),
                        asset.getId(), 4);
                return true;
            } catch (BusinessRuleException exception) {
                return false;
            }
        });
        CompletableFuture<Boolean> sale = CompletableFuture.supplyAsync(() -> {
            await(start);
            try {
                sales.sell(account.getId(), asset.getId(), origin.getId(), 4);
                return true;
            } catch (BusinessRuleException exception) {
                return false;
            }
        });
        start.countDown();

        boolean transferSucceeded = transfer.get(10, TimeUnit.SECONDS);
        boolean saleSucceeded = sale.get(10, TimeUnit.SECONDS);
        assertThat(List.of(transferSucceeded, saleSucceeded))
                .containsExactlyInAnyOrder(true, false);
        assertThat(position(origin).getQuantity()).isZero();
        assertThat(position(origin).getTotalCost()).isEqualByComparingTo("0.00");
        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(transferSucceeded ? "960.00" : "1080.00");

        var destinationPosition = positions.findByAccountIdAndAccountBrokerIdAndAssetId(
                account.getId(), destination.getId(), asset.getId());
        if (transferSucceeded) {
            assertThat(destinationPosition).isPresent();
            assertThat(destinationPosition.orElseThrow().getQuantity()).isEqualTo(4);
            assertThat(destinationPosition.orElseThrow().getTotalCost())
                    .isEqualByComparingTo("40.00");
        } else {
            assertThat(destinationPosition).isEmpty();
        }
        assertThat(movements.findAll().stream()
                .filter(item -> item.getMovementType() == MovementType.TRANSFER)).hasSize(
                        transferSucceeded ? 1 : 0);
        assertThat(movements.findAll().stream()
                .filter(item -> item.getMovementType() == MovementType.SALE)).hasSize(
                        saleSucceeded ? 1 : 0);
    }

    private CompletableFuture<Boolean> transferAfter(CountDownLatch start,
            AccountBroker target, long quantity) {
        return CompletableFuture.supplyAsync(() -> {
            await(start);
            try {
                transfers.transfer(account.getId(), origin.getId(), target.getId(), asset.getId(), quantity);
                return true;
            } catch (BusinessRuleException exception) {
                return false;
            }
        });
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private com.projeto.gestao.domain.model.Position position(AccountBroker association) {
        return positions.findByAccountIdAndAccountBrokerIdAndAssetId(account.getId(),
                association.getId(), asset.getId()).orElseThrow();
    }

    private Account saveAccount(String cpf, String email) {
        return accounts.save(Account.create(UUID.randomUUID(), "Investor", cpf, email, "hash",
                new BigDecimal("1000.00"), now()));
    }

    private AccountBroker association(Account owner, String cnpj, String name) {
        Broker broker = brokers.save(Broker.create(UUID.randomUUID(), cnpj, name + " SA", name,
                "ATIVA", "CTVM", "01001000", "Rua A", "1", null, "Centro", "São Paulo",
                "SP", now()));
        return associations.save(AccountBroker.create(UUID.randomUUID(), owner, broker, now()));
    }

    private static MarketQuote quote(String price) {
        return quote(price, 0);
    }

    private static MarketQuote quote(String price, long secondsAfterInitialQuote) {
        Instant quotedAt = INSTANT.plusSeconds(secondsAfterInitialQuote);
        return new MarketQuote("PETR4", "Petrobras", Market.BR, Currency.BRL,
                new BigDecimal(price), quotedAt, quotedAt.plusSeconds(1), "Brapi");
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.parse("2026-09-03T10:00:00-03:00");
    }
}
