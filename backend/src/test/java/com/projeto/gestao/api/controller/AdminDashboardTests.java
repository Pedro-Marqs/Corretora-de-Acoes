package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminRefreshTokenRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.security.AdminJwtTokenService;
import com.projeto.gestao.security.AdminPrincipal;
import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.security.JwtTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardTests {
    private static final String ADMIN_SECRET = testSecret();

    @Autowired private MockMvc mockMvc;
    @Autowired private AdminUserRepository adminUsers;
    @Autowired private AdminAuditEventRepository audits;
    @Autowired private AdminRefreshTokenRepository adminRefreshTokens;
    @Autowired private AccountRepository accounts;
    @Autowired private MovementRepository movements;
    @Autowired private ExchangeRateRepository exchangeRates;
    @Autowired private AdminJwtTokenService adminJwtTokenService;
    @Autowired private JwtTokenService investorJwtTokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

    private UUID adminId;
    private long cpfSequence;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.admin-jwt.enabled", () -> "true");
        registry.add("app.security.admin-jwt.issuer", () -> "corretora-admin-dashboard");
        registry.add("app.security.admin-jwt.audience", () -> "corretora-admin-dashboard-api");
        registry.add("app.security.admin-jwt.secret", () -> ADMIN_SECRET);
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.issuer", () -> "corretora-investor-dashboard");
        registry.add("app.security.jwt.audience", () -> "corretora-investor-dashboard-api");
        registry.add("app.security.jwt.secret", () -> testSecret());
    }

    @BeforeEach
    void setUp() {
        cleanup();
        adminId = UUID.randomUUID();
        cpfSequence = 1L;
        adminUsers.saveAndFlush(AdminUser.create(adminId,
                "dashboard-admin-" + adminId + "@example.com",
                passwordEncoder.encode("Senha Dashboard 1!"), OffsetDateTime.now()));
    }

    @Test
    void returnsOnlyAggregatedMetricsVolumesSeriesAndDeterministicRanking() throws Exception {
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        Account active = account("active-" + UUID.randomUUID() + "@example.com", now.minusDays(20));
        Account recent = account("recent-" + UUID.randomUUID() + "@example.com", now.minusDays(8));
        Account outsidePeriod = account("outside-" + UUID.randomUUID() + "@example.com", now.minusMonths(3));
        recent.inactivate(now.minusDays(2));
        outsidePeriod.inactivate(now.minusDays(2));
        accounts.saveAllAndFlush(java.util.List.of(active, recent, outsidePeriod));

        movements.saveAndFlush(Movement.purchase(UUID.randomUUID(), active, "PETR4", Market.BR,
                new BigDecimal("20.00"), new BigDecimal("20.00"), null, 3,
                new BigDecimal("60.00"), Currency.BRL, "Broker BR", new BigDecimal("9940.00"),
                now.minusDays(5)));
        movements.saveAndFlush(Movement.purchase(UUID.randomUUID(), recent, "AAPL", Market.US,
                new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("5.00"), 2,
                new BigDecimal("100.00"), Currency.USD, "Broker US", new BigDecimal("9900.00"),
                now.minusDays(4)));
        movements.saveAndFlush(Movement.sale(UUID.randomUUID(), active, "PETR4", Market.BR,
                new BigDecimal("22.00"), new BigDecimal("22.00"), null, 1,
                new BigDecimal("22.00"), Currency.BRL, "Broker BR", new BigDecimal("9962.00"),
                new BigDecimal("2.00"), now.minusDays(3)));
        exchangeRates.saveAndFlush(new ExchangeRate("USD/BRL", new BigDecimal("5.00"),
                now.minusDays(1), now.minusDays(1), "test"));

        mockMvc.perform(get("/api/admin/dashboard")
                        .param("period", "4W")
                        .param("limit", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(3))
                .andExpect(jsonPath("$.activeAccounts").value(1))
                .andExpect(jsonPath("$.inactiveAccounts").value(2))
                .andExpect(jsonPath("$.newAccounts").value(2))
                .andExpect(jsonPath("$.purchaseCount").value(2))
                .andExpect(jsonPath("$.saleCount").value(1))
                .andExpect(jsonPath("$.purchasedShares").value(5))
                .andExpect(jsonPath("$.soldShares").value(1))
                .andExpect(jsonPath("$.purchaseVolume.brl").value(60.00))
                .andExpect(jsonPath("$.purchaseVolume.usd").value(20.00))
                .andExpect(jsonPath("$.purchaseVolume.equivalentBrl").value(160.00))
                .andExpect(jsonPath("$.purchaseVolume.conversionStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.purchaseVolume.usdBrlRate").value(5.00))
                .andExpect(jsonPath("$.series.length()").value(5))
                .andExpect(jsonPath("$.ranking.length()").value(2))
                .andExpect(jsonPath("$.ranking[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.ranking[0].tradedShares").value(4))
                .andExpect(jsonPath("$.ranking[1].ticker").value("AAPL"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.balance").doesNotExist())
                .andExpect(jsonPath("$.accountId").doesNotExist());
    }

    @Test
    void preservesNativeUsdVolumeWhenConversionIsUnavailable() throws Exception {
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        Account account = account("usd-" + UUID.randomUUID() + "@example.com", now.minusDays(2));
        accounts.saveAndFlush(account);
        movements.saveAndFlush(Movement.purchase(UUID.randomUUID(), account, "AAPL", Market.US,
                new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("5.00"), 2,
                new BigDecimal("100.00"), Currency.USD, "Broker US", new BigDecimal("9900.00"),
                now.minusDays(1)));

        mockMvc.perform(get("/api/admin/dashboard").param("period", "MAX")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseVolume.usd").value(20.00))
                .andExpect(jsonPath("$.purchaseVolume.brl").value(0.00))
                .andExpect(jsonPath("$.purchaseVolume.equivalentBrl").doesNotExist())
                .andExpect(jsonPath("$.purchaseVolume.conversionStatus").value("UNAVAILABLE"));
    }

    @Test
    void reportsUnavailableWhenExchangeRateIsStale() throws Exception {
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        Account account = account("stale-rate-" + UUID.randomUUID() + "@example.com",
                now.minusDays(2));
        accounts.saveAndFlush(account);
        movements.saveAndFlush(Movement.purchase(UUID.randomUUID(), account, "AAPL", Market.US,
                new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("5.00"), 2,
                new BigDecimal("100.00"), Currency.USD, "Broker US", new BigDecimal("9900.00"),
                now.minusDays(1)));
        ExchangeRate rate = new ExchangeRate("USD/BRL", new BigDecimal("5.00"),
                now.minusDays(8), now.minusDays(8), "test");
        exchangeRates.saveAndFlush(rate);

        mockMvc.perform(get("/api/admin/dashboard").param("period", "MAX")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseVolume.equivalentBrl").doesNotExist())
                .andExpect(jsonPath("$.purchaseVolume.conversionStatus").value("UNAVAILABLE"));
    }

    @Test
    void rejectsInvestorsAndInvalidPeriodsOrLimits() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").param("period", "4W"))
                .andExpect(status().isUnauthorized());
        Account investor = account("investor-" + UUID.randomUUID() + "@example.com",
                OffsetDateTime.now().minusDays(1));
        accounts.saveAndFlush(investor);
        String investorToken = investorJwtTokenService.issue(
                new AccountPrincipal(investor.getId())).tokenValue();
        mockMvc.perform(get("/api/admin/dashboard").param("period", "4W")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + investorToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/dashboard").param("period", "bad")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/dashboard").param("period", "4W").param("limit", "51")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void appliesRankingLimitAndDeterministicTickerTieBreaker() throws Exception {
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        Account account = account("ranking-" + UUID.randomUUID() + "@example.com", now.minusDays(1));
        accounts.saveAndFlush(account);
        movements.saveAndFlush(Movement.purchase(UUID.randomUUID(), account, "BETA", Market.BR,
                new BigDecimal("10.00"), new BigDecimal("10.00"), null, 2,
                new BigDecimal("20.00"), Currency.BRL, "Broker", new BigDecimal("9980.00"),
                now.minusHours(2)));
        movements.saveAndFlush(Movement.purchase(UUID.randomUUID(), account, "ALFA", Market.US,
                new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("5.00"), 2,
                new BigDecimal("100.00"), Currency.USD, "Broker", new BigDecimal("9880.00"),
                now.minusHours(1)));

        mockMvc.perform(get("/api/admin/dashboard").param("period", "4W").param("limit", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ranking.length()").value(1))
                .andExpect(jsonPath("$.ranking[0].ticker").value("ALFA"));
    }

    @Test
    void appliesInclusiveStartAndExclusiveEndOfThePeriod() throws Exception {
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        Account inside = account("inside-boundary-" + UUID.randomUUID() + "@example.com",
                now.minusWeeks(4).plusSeconds(5));
        Account outside = account("outside-boundary-" + UUID.randomUUID() + "@example.com",
                now.minusWeeks(4).minusSeconds(5));
        accounts.saveAllAndFlush(java.util.List.of(inside, outside));

        mockMvc.perform(get("/api/admin/dashboard").param("period", "4W")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(2))
                .andExpect(jsonPath("$.activeAccounts").value(2))
                .andExpect(jsonPath("$.inactiveAccounts").value(0))
                .andExpect(jsonPath("$.newAccounts").value(1));
    }

    @Test
    void groupsDailySeriesByBrasiliaCalendarDay() throws Exception {
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        OffsetDateTime brasiliaLate = now.minusDays(1).withHour(23).withMinute(30)
                .withSecond(0);
        OffsetDateTime utcAfterMidnight = brasiliaLate.withOffsetSameInstant(ZoneOffset.UTC);
        Account account = account("brasilia-day-" + UUID.randomUUID() + "@example.com",
                utcAfterMidnight);
        accounts.saveAndFlush(account);

        mockMvc.perform(get("/api/admin/dashboard").param("period", "MAX")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series.length()").value(1))
                .andExpect(jsonPath("$.series[0].date").value(brasiliaLate.toLocalDate().toString()))
                .andExpect(jsonPath("$.series[0].newAccounts").value(1));
    }

    @Test
    void maxStartsAtTheFirstRelevantAccountData() throws Exception {
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        Account account = account("max-period-" + UUID.randomUUID() + "@example.com",
                now.minusMonths(3));
        accounts.saveAndFlush(account);

        mockMvc.perform(get("/api/admin/dashboard").param("period", "MAX")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(1))
                .andExpect(jsonPath("$.activeAccounts").value(1))
                .andExpect(jsonPath("$.inactiveAccounts").value(0))
                .andExpect(jsonPath("$.newAccounts").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"4W", "3M", "6M", "1Y", "5Y", "MAX"})
    void acceptsEverySupportedPeriod(String period) throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").param("period", period)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void returnsEmptyAggregatesWithoutInternalError() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").param("period", "MAX")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(0))
                .andExpect(jsonPath("$.activeAccounts").value(0))
                .andExpect(jsonPath("$.inactiveAccounts").value(0))
                .andExpect(jsonPath("$.newAccounts").value(0))
                .andExpect(jsonPath("$.purchaseCount").value(0))
                .andExpect(jsonPath("$.saleCount").value(0))
                .andExpect(jsonPath("$.purchasedShares").value(0))
                .andExpect(jsonPath("$.soldShares").value(0))
                .andExpect(jsonPath("$.series").isEmpty())
                .andExpect(jsonPath("$.ranking").isEmpty());
    }

    private Account account(String email, OffsetDateTime createdAt) {
        String cpf = String.format("%011d", cpfSequence++);
        return Account.create(UUID.randomUUID(), "Dashboard Investor", cpf, email,
                passwordEncoder.encode("Senha Investidor 1!"), new BigDecimal("10000.00"), createdAt);
    }

    private String adminToken() {
        return adminJwtTokenService.issue(new AdminPrincipal(adminId)).tokenValue();
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
        adminRefreshTokens.deleteAll();
        audits.deleteAll();
        adminUsers.deleteAll();
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
