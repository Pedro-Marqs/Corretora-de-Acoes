package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.MovementRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AdminDashboardPostgreSqlTests {
    private static final String TEST_SECRET = testSecret();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private AccountRepository accounts;
    @Autowired private MovementRepository movements;
    @Autowired private ExchangeRateRepository exchangeRates;
    @Autowired private AdminDashboardService dashboardService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

    private Account account;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("app.security.jwt.enabled", () -> "false");
        registry.add("app.security.admin-jwt.enabled", () -> "false");
        registry.add("app.security.admin-jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void setUp() {
        cleanup();
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        account = Account.create(UUID.randomUUID(), "Dashboard PostgreSQL", "52998224725",
                "dashboard-postgres-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("Senha 1!"), new BigDecimal("10000.00"), now.minusDays(2));
        Account outsidePeriod = Account.create(UUID.randomUUID(), "Dashboard PostgreSQL Old",
                "11144477735", "dashboard-postgres-old-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("Senha 1!"), new BigDecimal("10000.00"), now.minusMonths(3));
        outsidePeriod.inactivate(now.minusDays(2));
        accounts.saveAllAndFlush(List.of(account, outsidePeriod));
        movements.saveAndFlush(Movement.purchase(UUID.randomUUID(), account, "PETR4", Market.BR,
                new BigDecimal("20.00"), new BigDecimal("20.00"), null, 3,
                new BigDecimal("60.00"), Currency.BRL, "Broker", new BigDecimal("9940.00"),
                now.minusDays(1)));
        movements.saveAndFlush(Movement.purchase(UUID.randomUUID(), account, "AAPL", Market.US,
                new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("5.00"), 2,
                new BigDecimal("100.00"), Currency.USD, "Broker", new BigDecimal("9840.00"),
                now.minusHours(12)));
        exchangeRates.saveAndFlush(new ExchangeRate("USD/BRL", new BigDecimal("5.00"),
                now.minusDays(1), now.minusDays(1), "test"));
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void aggregatesAdministrativeDashboardOnPostgreSqlWithoutLoadingFinancialEntities() {
        var response = dashboardService.query(DashboardPeriod.FOUR_WEEKS, 1);

        assertThat(response.totalAccounts()).isEqualTo(2);
        assertThat(response.activeAccounts()).isEqualTo(1);
        assertThat(response.inactiveAccounts()).isEqualTo(1);
        assertThat(response.newAccounts()).isEqualTo(1);
        assertThat(response.purchaseCount()).isEqualTo(2);
        assertThat(response.purchasedShares()).isEqualTo(5);
        assertThat(response.purchaseVolume().brl()).isEqualByComparingTo("60.00");
        assertThat(response.purchaseVolume().usd()).isEqualByComparingTo("20.00");
        assertThat(response.purchaseVolume().equivalentBrl()).isEqualByComparingTo("160.00");
        assertThat(response.ranking()).hasSize(1);
        assertThat(response.series()).hasSize(2);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM patrimonial_point");
        jdbc.update("DELETE FROM movement");
        jdbc.update("DELETE FROM position");
        jdbc.update("DELETE FROM account_broker");
        jdbc.update("DELETE FROM quote");
        jdbc.update("DELETE FROM exchange_rate");
        jdbc.update("DELETE FROM asset");
        jdbc.update("DELETE FROM broker");
        jdbc.update("DELETE FROM account");
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
