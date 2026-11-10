package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditEvent;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminRefreshTokenRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.repository.RefreshTokenRepository;
import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.security.AdminJwtTokenService;
import com.projeto.gestao.security.AdminPrincipal;
import com.projeto.gestao.security.JwtTokenService;
import com.projeto.gestao.service.RefreshTokenService;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.hibernate.resource.jdbc.spi.StatementInspector;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminInvestorManagementTests {
    private static final String ADMIN_PASSWORD = "Senha Admin Gestão 1!";
    private static final String INVESTOR_PASSWORD = "Senha Investidor Gestão 1!";
    private static final String ADMIN_SECRET = testSecret();
    private static final String INVESTOR_SECRET = testSecret();
    private static final List<String> SQL_STATEMENTS = new CopyOnWriteArrayList<>();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminAuditEventRepository auditRepository;
    @Autowired private AdminRefreshTokenRepository adminRefreshTokenRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private AdminJwtTokenService adminJwtTokenService;
    @Autowired private JwtTokenService investorJwtTokenService;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private UUID adminId;
    private UUID accountId;
    private UUID secondAccountId;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "false");
        registry.add("app.security.jwt.issuer", () -> "corretora-investor-management");
        registry.add("app.security.jwt.audience", () -> "corretora-investors-management");
        registry.add("app.security.jwt.secret", () -> INVESTOR_SECRET);
        registry.add("app.security.admin-jwt.enabled", () -> "true");
        registry.add("app.security.admin-jwt.issuer", () -> "corretora-admin-management");
        registry.add("app.security.admin-jwt.audience", () -> "corretora-admin-api-management");
        registry.add("app.security.admin-jwt.secret", () -> ADMIN_SECRET);
        registry.add("spring.jpa.properties.hibernate.session_factory.statement_inspector",
                SqlCaptureInspector.class::getName);
    }

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        adminRefreshTokenRepository.deleteAll();
        auditRepository.deleteAll();
        accountRepository.deleteAll();
        adminUserRepository.deleteAll();

        adminId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        secondAccountId = UUID.randomUUID();
        adminUserRepository.saveAndFlush(AdminUser.create(
                adminId, "admin.management@example.com", passwordEncoder.encode(ADMIN_PASSWORD),
                OffsetDateTime.parse("2026-01-10T10:00:00Z")));
        accountRepository.saveAndFlush(account(accountId, "Ana Investidora", "52998224725",
                "ana@example.com", "2026-01-01T10:00:00Z"));
        accountRepository.saveAndFlush(account(secondAccountId, "Bruno Investidor", "11144477735",
                "bruno@example.com", "2026-02-01T10:00:00Z"));
        SQL_STATEMENTS.clear();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        adminRefreshTokenRepository.deleteAll();
        auditRepository.deleteAll();
        accountRepository.deleteAll();
        adminUserRepository.deleteAll();
    }

    @Test
    void adminListsMaskedInvestorsWithWhitelistedPaginationAndNoFinancialFields() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/admin/investors")
                        .param("status", "ALL")
                        .param("search", "Ana")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "name,asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-Correlation-ID", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(accountId.toString()))
                .andExpect(jsonPath("$.content[0].emailMasked").value("a***@example.com"))
                .andExpect(jsonPath("$.content[0].cpfMasked").value("529.***.***-25"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].balance").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));

        String accountProjectionQuery = SQL_STATEMENTS.stream()
                .map(String::toLowerCase)
                .filter(sql -> sql.startsWith("select") && sql.contains(" from account ")
                        && !sql.contains("count("))
                .findFirst().orElseThrow();
        assertThat(accountProjectionQuery)
                .contains("id", "name", "email", "cpf", "status", "created_at", "inactivated_at")
                .doesNotContain("password_hash", "balance");

        AdminAuditEvent audit = lastAudit(AdminAuditAction.LIST_INVESTORS);
        assertThat(audit.getActorAdminId()).isEqualTo(adminId);
        assertThat(audit.getOutcome()).isEqualTo(AdminAuditOutcome.SUCCESS);
    }

    @Test
    void invalidPaginationFiltersDatesAndSortAreRejected() throws Exception {
        String token = adminToken();
        List<Map<String, String>> invalid = List.of(
                Map.of("size", "0"), Map.of("size", "101"), Map.of("page", "-1"),
                Map.of("status", "DELETED"), Map.of("sort", "password,asc"),
                Map.of("createdFrom", "2026-03-01T00:00:00Z",
                        "createdTo", "2026-02-01T00:00:00Z"));

        for (Map<String, String> parameters : invalid) {
            var request = get("/api/admin/investors")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            parameters.forEach(request::param);
            mockMvc.perform(request).andExpect(status().isBadRequest());
        }

        List<AdminAuditEvent> listAudits = auditRepository.findAll().stream()
                .filter(event -> event.getAction() == AdminAuditAction.LIST_INVESTORS)
                .toList();
        assertThat(listAudits).hasSize(invalid.size());
        assertThat(listAudits).allSatisfy(event -> {
            assertThat(event.getActorAdminId()).isEqualTo(adminId);
            assertThat(event.getOutcome()).isEqualTo(AdminAuditOutcome.DENIED);
        });
    }

    @Test
    void investorCannotListAdministrativeInvestors() throws Exception {
        String token = investorJwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();

        mockMvc.perform(get("/api/admin/investors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminDeactivationIsTransactionalAndRevokesInvestorCredentialsOnlyForTarget() throws Exception {
        String adminToken = adminToken();
        String investorAccess = investorJwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();
        RefreshTokenService.IssuedRefreshToken refresh =
                refreshTokenService.issue(new AccountPrincipal(accountId));
        BigDecimal secondBalance = accountRepository.findById(secondAccountId).orElseThrow().getBalance();

        mockMvc.perform(patch("/api/admin/investors/{id}/status", accountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isNoContent());

        assertThat(accountRepository.findById(accountId).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.INACTIVE);
        assertThat(accountRepository.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
        assertThat(accountRepository.findById(secondAccountId).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
        assertThat(accountRepository.findById(secondAccountId).orElseThrow().getBalance())
                .isEqualByComparingTo(secondBalance);
        assertThat(refreshTokenRepository.findAll()).allMatch(token -> token.getRevokedAt() != null);

        mockMvc.perform(get("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + investorAccess))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").with(csrf())
                        .cookie(new Cookie("REFRESH_TOKEN", refresh.tokenValue())))
                .andExpect(status().isUnauthorized());

        assertThat(lastAudit(AdminAuditAction.DEACTIVATE_INVESTOR).getTargetAccountId())
                .isEqualTo(accountId);
    }

    @Test
    void activationWorksAndRepeatedTransitionsReturnConflictWithoutChangingState() throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.inactivate(OffsetDateTime.parse("2026-02-10T10:00:00Z"));
        accountRepository.saveAndFlush(account);
        String token = adminToken();

        mockMvc.perform(patch("/api/admin/investors/{id}/status", accountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/admin/investors/{id}/status", accountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict());

        assertThat(accountRepository.findById(accountId).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
        assertThat(lastAudit(AdminAuditAction.ACCESS_DENIED).getOutcome())
                .isEqualTo(AdminAuditOutcome.DENIED);
    }

    @Test
    void missingInvestorReturnsNotFoundAndDoesNotCreateForeignAuditTarget() throws Exception {
        String token = adminToken();
        UUID missing = UUID.randomUUID();

        mockMvc.perform(patch("/api/admin/investors/{id}/status", missing)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isNotFound());

        AdminAuditEvent audit = lastAudit(AdminAuditAction.ACCESS_DENIED);
        assertThat(audit.getTargetAccountId()).isNull();
        assertThat(accountRepository.findById(accountId).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void administrativeLoginLogoutAndRejectedLoginAreAuditedWithoutSecrets() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/admin/auth/login")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", UUID.randomUUID().toString())
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "admin.management@example.com",
                                        "password", ADMIN_PASSWORD))))
                .andExpect(status().isNoContent()).andReturn();

        assertThat(lastAudit(AdminAuditAction.LOGIN).getActorAdminId()).isEqualTo(adminId);
        Cookie refresh = login.getResponse().getCookie(AdminAuthenticationController.REFRESH_COOKIE);
        mockMvc.perform(post("/api/admin/auth/logout").with(csrf()).cookie(refresh))
                .andExpect(status().isNoContent());
        assertThat(lastAudit(AdminAuditAction.LOGOUT).getActorAdminId()).isEqualTo(adminId);

        mockMvc.perform(post("/api/admin/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "admin.management@example.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
        assertThat(lastAudit(AdminAuditAction.ACCESS_DENIED).getOutcome())
                .isEqualTo(AdminAuditOutcome.DENIED);
        assertThat(objectMapper.writeValueAsString(auditRepository.findAll()))
                .doesNotContain(ADMIN_PASSWORD, "passwordHash", "refreshToken", "cookie");
    }

    private String adminToken() {
        return adminJwtTokenService.issue(new AdminPrincipal(adminId)).tokenValue();
    }

    private AdminAuditEvent lastAudit(AdminAuditAction action) {
        return auditRepository.findAll().stream()
                .filter(event -> event.getAction() == action)
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    private Account account(UUID id, String name, String cpf, String email, String createdAt) {
        return Account.create(id, name, cpf, email, passwordEncoder.encode(INVESTOR_PASSWORD),
                new BigDecimal("10000.00"), OffsetDateTime.parse(createdAt));
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static final class SqlCaptureInspector implements StatementInspector {
        private static final long serialVersionUID = 1L;

        @Override
        public String inspect(String sql) {
            SQL_STATEMENTS.add(sql);
            return sql;
        }
    }
}
