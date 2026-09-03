package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_POSTGRES_TESTS", matches = "(?i)true")
class LocalPostgreSqlPurchaseTests {
    private static final String DATABASE_NAME = "gestao_acoes_test";

    @DynamicPropertySource
    static void localPostgres(DynamicPropertyRegistry registry) {
        String url = System.getenv().getOrDefault(
                "LOCAL_POSTGRES_TEST_URL", "jdbc:postgresql://localhost:5432/" + DATABASE_NAME);
        if (!url.matches("jdbc:postgresql://[^/]+/" + DATABASE_NAME + "(?:\\?.*)?")) {
            throw new IllegalStateException("O teste aceita exclusivamente o banco " + DATABASE_NAME);
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> required("LOCAL_POSTGRES_TEST_USER"));
        registry.add("spring.datasource.password", () -> required("LOCAL_POSTGRES_TEST_PASSWORD"));
        registry.add("spring.flyway.locations", () ->
                "classpath:db/migration/common,classpath:db/migration/postgresql");
    }

    @Autowired private PurchaseService service;
    @Autowired private AccountRepository accounts;
    @Autowired private BrokerRepository brokers;
    @Autowired private AccountBrokerRepository associations;
    @Autowired private AssetRepository assets;
    @Autowired private PositionRepository positions;
    @Autowired private MovementRepository movements;
    @Autowired private PatrimonialPointRepository points;
    @MockitoBean private BrazilMarketDataPort brazil;

    @Test
    void persistsLockedPurchaseAtomicallyInIsolatedLocalPostgreSql() {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-03T10:00:00-03:00");
        Account account = accounts.save(Account.create(UUID.randomUUID(), "Investor", "52998224725",
                "t27-postgres@example.com", "hash", new BigDecimal("1000.00"), now));
        Broker broker = brokers.save(Broker.create(UUID.randomUUID(), "02332886000104", "XP SA", "XP",
                "ATIVA", "CTVM", "01001000", "Rua A", "1", null, "Centro", "São Paulo", "SP", now));
        AccountBroker association = associations.save(AccountBroker.create(
                UUID.randomUUID(), account, broker, now));
        Asset asset = assets.save(new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL));
        when(brazil.findQuote("PETR4")).thenReturn(new MarketQuote("PETR4", "Petrobras", Market.BR,
                Currency.BRL, new BigDecimal("50.00"), Instant.parse("2026-09-03T13:00:00Z"),
                Instant.parse("2026-09-03T13:00:01Z"), "provider"));

        PurchaseResult result = service.purchase(account.getId(), asset.getId(), association.getId(), 2);

        assertThat(result.remainingBalanceBrl()).isEqualByComparingTo("900.00");
        assertThat(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0)).hasSize(1);
        assertThat(movements.count()).isEqualTo(1);
        assertThat(points.count()).isEqualTo(1);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Variável ausente: " + name);
        return value;
    }
}
