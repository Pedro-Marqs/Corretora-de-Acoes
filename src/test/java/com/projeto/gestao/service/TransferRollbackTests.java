package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.PatrimonialPoint;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
class TransferRollbackTests {
    @Autowired private PurchaseService purchases;
    @Autowired private TransferService transfers;
    @Autowired private AccountRepository accounts;
    @Autowired private BrokerRepository brokers;
    @Autowired private AccountBrokerRepository associations;
    @Autowired private AssetRepository assets;
    @Autowired private QuoteRepository quotes;
    @MockitoSpyBean private PositionRepository positions;
    @MockitoSpyBean private MovementRepository movements;
    @MockitoSpyBean private PatrimonialPointRepository points;
    @MockitoBean private BrazilMarketDataPort brazil;

    private UUID accountId;
    private UUID assetId;
    private UUID originId;
    private UUID destinationId;

    @BeforeEach
    void setUp() {
        cleanup();
        Account account = accounts.save(Account.create(UUID.randomUUID(), "Investor",
                "52998224725", "rollback-transfer@example.com", "hash",
                new BigDecimal("1000.00"), now()));
        originId = association(account, "02332886000104", "Origem").getId();
        destinationId = association(account, "10270580000110", "Destino").getId();
        Asset asset = assets.save(new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL));
        accountId = account.getId();
        assetId = asset.getId();
        when(brazil.findQuote("PETR4")).thenReturn(new MarketQuote("PETR4", "Petrobras",
                Market.BR, Currency.BRL, new BigDecimal("10.00"),
                Instant.parse("2026-09-03T13:00:00Z"),
                Instant.parse("2026-09-03T13:00:01Z"), "provider"));
        purchases.purchase(accountId, assetId, originId, 4);
    }

    @AfterEach
    void cleanup() {
        points.deleteAll();
        movements.deleteAll();
        positions.deleteAll();
        quotes.deleteAll();
        associations.deleteAll();
        brokers.deleteAll();
        assets.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void rollsBackOriginWhenCreatingDestinationPositionFails() {
        doThrow(new IllegalStateException("simulated destination failure"))
                .when(positions).save(any(Position.class));

        assertThatThrownBy(() -> transfer()).isInstanceOf(IllegalStateException.class);

        assertUnchanged();
    }

    @Test
    void rollsBackPositionsWhenMovementFails() {
        doThrow(new IllegalStateException("simulated movement failure"))
                .when(movements).save(any(Movement.class));

        assertThatThrownBy(() -> transfer()).isInstanceOf(IllegalStateException.class);

        assertUnchanged();
    }

    @Test
    void rollsBackPositionsAndMovementWhenPatrimonyFails() {
        doThrow(new IllegalStateException("simulated point failure"))
                .when(points).save(any(PatrimonialPoint.class));

        assertThatThrownBy(() -> transfer()).isInstanceOf(IllegalStateException.class);

        assertUnchanged();
    }

    private void transfer() {
        transfers.transfer(accountId, originId, destinationId, assetId, 2);
    }

    private void assertUnchanged() {
        Position origin = positions.findByAccountIdAndAccountBrokerIdAndAssetId(
                accountId, originId, assetId).orElseThrow();
        assertThat(origin.getQuantity()).isEqualTo(4);
        assertThat(origin.getAveragePrice()).isEqualByComparingTo("10.00");
        assertThat(origin.getTotalCost()).isEqualByComparingTo("40.00");
        assertThat(positions.findByAccountIdAndAccountBrokerIdAndAssetId(
                accountId, destinationId, assetId)).isEmpty();
        assertThat(accounts.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("960.00");
        assertThat(movements.count()).isEqualTo(1);
        assertThat(points.count()).isEqualTo(1);
    }

    private AccountBroker association(Account account, String cnpj, String name) {
        Broker broker = brokers.save(Broker.create(UUID.randomUUID(), cnpj, name + " SA", name,
                "ATIVA", "CTVM", "01001000", "Rua A", "1", null, "Centro", "São Paulo",
                "SP", now()));
        return associations.save(AccountBroker.create(UUID.randomUUID(), account, broker, now()));
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.parse("2026-09-03T10:00:00-03:00");
    }
}
