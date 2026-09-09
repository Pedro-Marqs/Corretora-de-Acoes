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
class SaleRollbackTests {
    @Autowired private PurchaseService purchases;
    @Autowired private SaleService sales;
    @Autowired private AccountRepository accounts;
    @Autowired private BrokerRepository brokers;
    @Autowired private AccountBrokerRepository associations;
    @Autowired private AssetRepository assets;
    @Autowired private QuoteRepository quotes;
    @Autowired private PositionRepository positions;
    @MockitoSpyBean private MovementRepository movements;
    @MockitoSpyBean private PatrimonialPointRepository points;
    @MockitoBean private BrazilMarketDataPort brazil;
    private UUID accountId;
    private UUID assetId;
    private UUID associationId;

    @BeforeEach
    void setUp() {
        cleanup();
        Account account = accounts.save(Account.create(UUID.randomUUID(), "Investor", "52998224725",
                "rollback-sale@example.com", "hash", new BigDecimal("1000.00"), now()));
        Broker broker = brokers.save(Broker.create(UUID.randomUUID(), "02332886000104", "XP SA", "XP",
                "ATIVA", "CTVM", "01001000", "Rua A", "1", null, "Centro", "São Paulo", "SP", now()));
        AccountBroker association = associations.save(AccountBroker.create(UUID.randomUUID(), account, broker, now()));
        Asset asset = assets.save(new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL));
        accountId = account.getId(); assetId = asset.getId(); associationId = association.getId();
        when(brazil.findQuote("PETR4")).thenReturn(marketQuote("25.00", Instant.parse("2026-09-03T13:00:00Z")),
                marketQuote("30.00", Instant.parse("2026-09-03T13:05:00Z")));
        purchases.purchase(accountId, assetId, associationId, 4);
    }

    @AfterEach
    void cleanup() {
        points.deleteAll(); movements.deleteAll(); positions.deleteAll(); quotes.deleteAll();
        associations.deleteAll(); brokers.deleteAll(); assets.deleteAll(); accounts.deleteAll();
    }

    @Test
    void rollsBackCreditAndPositionWhenMovementFails() {
        doThrow(new IllegalStateException("simulated movement failure")).when(movements).save(any());
        assertThatThrownBy(() -> sales.sell(accountId, assetId, associationId, 2))
                .isInstanceOf(IllegalStateException.class);
        assertUnchanged();
    }

    @Test
    void rollsBackCreditPositionAndMovementWhenPatrimonyFails() {
        doThrow(new IllegalStateException("simulated point failure")).when(points).save(any());
        assertThatThrownBy(() -> sales.sell(accountId, assetId, associationId, 2))
                .isInstanceOf(IllegalStateException.class);
        assertUnchanged();
    }

    private void assertUnchanged() {
        assertThat(accounts.findById(accountId).orElseThrow().getBalance()).isEqualByComparingTo("900.00");
        assertThat(positions.findByAccountIdAndAccountBrokerIdAndAssetId(accountId, associationId, assetId)
                .orElseThrow().getQuantity()).isEqualTo(4);
        assertThat(movements.count()).isEqualTo(1);
        assertThat(points.count()).isEqualTo(1);
    }

    private static MarketQuote marketQuote(String price, Instant instant) {
        return new MarketQuote("PETR4", "Petrobras", Market.BR, Currency.BRL, new BigDecimal(price),
                instant, instant.plusSeconds(1), "Brapi");
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.parse("2026-09-03T10:00:00-03:00");
    }
}
