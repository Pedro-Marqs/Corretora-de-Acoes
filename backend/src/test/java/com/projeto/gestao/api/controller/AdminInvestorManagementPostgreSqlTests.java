package com.projeto.gestao.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminRefreshTokenRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.repository.RefreshTokenRepository;
import com.projeto.gestao.security.AdminJwtTokenService;
import com.projeto.gestao.security.AdminPrincipal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AdminInvestorManagementPostgreSqlTests {
    private static final String ADMIN_PASSWORD = "Senha Admin PostgreSQL 1!";
    private static final String TEST_SECRET = testSecret();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminAuditEventRepository auditRepository;
    @Autowired private AdminRefreshTokenRepository adminRefreshTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private AdminJwtTokenService adminJwtTokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

    private UUID adminId;
    private UUID activeAccountId;
    private UUID inactiveAccountId;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("app.security.jwt.enabled", () -> "false");
        registry.add("app.security.admin-jwt.enabled", () -> "true");
        registry.add("app.security.admin-jwt.issuer", () -> "corretora-admin-postgresql");
        registry.add("app.security.admin-jwt.audience", () -> "corretora-admin-api-postgresql");
        registry.add("app.security.admin-jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void setUp() {
        cleanup();
        OffsetDateTime now = OffsetDateTime.now().withNano(0);
        adminId = UUID.randomUUID();
        activeAccountId = UUID.randomUUID();
        inactiveAccountId = UUID.randomUUID();
        adminUserRepository.saveAndFlush(AdminUser.create(
                adminId, "admin-postgresql@example.com", passwordEncoder.encode(ADMIN_PASSWORD), now));

        accountRepository.saveAndFlush(Account.create(
                activeAccountId, "Ana PostgreSQL", "52998224725", "ana-postgresql@example.com",
                passwordEncoder.encode("Senha Investidor 1!"), new BigDecimal("10000.00"),
                now.minusDays(2)));
        Account inactive = Account.create(
                inactiveAccountId, "Bruno PostgreSQL", "11144477735", "bruno-postgresql@example.com",
                passwordEncoder.encode("Senha Investidor 1!"), new BigDecimal("10000.00"),
                now.minusDays(5));
        inactive.inactivate(now.minusDays(2));
        accountRepository.saveAndFlush(inactive);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void listsWithEmptySearchAndNullOptionalDatesOnPostgreSqlAndAlsoWithFilledFilters() throws Exception {
        String token = adminJwtTokenService.issue(new AdminPrincipal(adminId)).tokenValue();
        OffsetDateTime now = OffsetDateTime.now().withNano(0);

        mockMvc.perform(get("/api/admin/investors")
                        .param("status", "ALL")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "name,asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Ana PostgreSQL"))
                .andExpect(jsonPath("$.content[0].emailMasked").value("a***@example.com"))
                .andExpect(jsonPath("$.content[0].cpfMasked").value("529.***.***-25"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/admin/investors")
                        .param("status", "INACTIVE")
                        .param("search", "Bruno")
                        .param("createdFrom", now.minusDays(10).toString())
                        .param("createdTo", now.toString())
                        .param("inactivatedFrom", now.minusDays(3).toString())
                        .param("inactivatedTo", now.toString())
                        .param("sort", "createdAt,desc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(inactiveAccountId.toString()))
                .andExpect(jsonPath("$.content[0].emailMasked").value("b***@example.com"))
                .andExpect(jsonPath("$.content[0].cpfMasked").value("111.***.***-35"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private void cleanup() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM admin_audit_event");
        jdbc.update("DELETE FROM admin_refresh_token");
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM patrimonial_point");
        jdbc.update("DELETE FROM movement");
        jdbc.update("DELETE FROM position");
        jdbc.update("DELETE FROM account_broker");
        jdbc.update("DELETE FROM quote");
        jdbc.update("DELETE FROM exchange_rate");
        jdbc.update("DELETE FROM asset");
        jdbc.update("DELETE FROM broker");
        jdbc.update("DELETE FROM account");
        jdbc.update("DELETE FROM admin_user");
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
