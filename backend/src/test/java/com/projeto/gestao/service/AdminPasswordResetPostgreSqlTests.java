package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditEvent;
import com.projeto.gestao.domain.model.AdminRefreshToken;
import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminRefreshTokenRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.security.AdminPrincipal;

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
class AdminPasswordResetPostgreSqlTests {
    private static final String TEST_SECRET = testSecret();
    private static final OffsetDateTime INACTIVATED_AT =
            OffsetDateTime.parse("2026-08-17T13:00:00.123456Z");
    private static final String REFRESH_FAILURE_FUNCTION =
            "fail_admin_password_reset_refresh";
    private static final String AUDIT_FAILURE_FUNCTION =
            "fail_admin_password_reset_audit";
    private static final String REFRESH_FAILURE_TRIGGER =
            "trg_fail_admin_password_reset_refresh";
    private static final String AUDIT_FAILURE_TRIGGER =
            "trg_fail_admin_password_reset_audit";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private AccountRepository accountRepository;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminRefreshTokenRepository refreshTokenRepository;
    @Autowired private AdminAuditEventRepository auditRepository;
    @Autowired private AdminPasswordResetService resetService;
    @Autowired private AdminRefreshTokenService refreshTokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID targetAdminId;
    private UUID otherAdminId;
    private UUID investorId;
    private String targetEmail;
    private String targetOldHash;
    private String otherAdminHash;
    private String investorHash;

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
        registry.add("app.security.admin-jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void setUp() {
        removeFailureTriggers();
        refreshTokenRepository.deleteAll();
        auditRepository.deleteAll();
        adminUserRepository.deleteAll();
        accountRepository.deleteAll();

        targetAdminId = UUID.randomUUID();
        otherAdminId = UUID.randomUUID();
        investorId = UUID.randomUUID();
        targetEmail = "reset-target-" + targetAdminId + "@example.com";

        AdminUser target = AdminUser.create(targetAdminId, targetEmail,
                passwordEncoder.encode("old-admin-password"), OffsetDateTime.now());
        AdminUser other = AdminUser.create(otherAdminId,
                "reset-other-" + otherAdminId + "@example.com",
                passwordEncoder.encode("other-admin-password"), OffsetDateTime.now());
        Account investor = Account.create(investorId, "Reset Test Investor", "52998224725",
                "reset-investor-" + investorId + "@example.com",
                passwordEncoder.encode("investor-password"), new BigDecimal("10000.00"),
                OffsetDateTime.now());

        adminUserRepository.saveAllAndFlush(List.of(target, other));
        accountRepository.saveAndFlush(investor);
        targetOldHash = target.passwordHash();
        otherAdminHash = other.passwordHash();
        investorHash = investor.getPasswordHash();
    }

    @AfterEach
    void tearDown() {
        removeFailureTriggers();
        refreshTokenRepository.deleteAll();
        auditRepository.deleteAll();
        adminUserRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void successfulResetUpdatesOnlyTargetRevokesSessionsAndAuditsSystem() {
        refreshTokenService.issue(new AdminPrincipal(targetAdminId));
        refreshTokenService.issue(new AdminPrincipal(otherAdminId));
        List<AdminRefreshToken> tokensBefore = refreshTokenRepository.findAll();

        resetService.reset(targetEmail, "new-admin-password".toCharArray());

        AdminUser target = adminUserRepository.findById(targetAdminId).orElseThrow();
        AdminUser other = adminUserRepository.findById(otherAdminId).orElseThrow();
        Account investor = accountRepository.findById(investorId).orElseThrow();
        List<AdminRefreshToken> tokens = refreshTokenRepository.findAll();
        AdminAuditEvent audit = auditRepository.findAll().stream()
                .filter(event -> event.getAction() == AdminAuditAction.RESET_ADMIN_PASSWORD)
                .findFirst().orElseThrow();

        assertThat(passwordEncoder.matches("new-admin-password", target.passwordHash())).isTrue();
        assertThat(passwordEncoder.matches("old-admin-password", target.passwordHash())).isFalse();
        assertThat(other.passwordHash()).isEqualTo(otherAdminHash);
        assertThat(investor.getPasswordHash()).isEqualTo(investorHash);
        assertThat(investor.getBalance()).isEqualByComparingTo("10000.00");
        assertTokenRowsUnchanged(tokensBefore, tokens);
        assertThat(tokens).filteredOn(token -> token.getAdminUserId().equals(targetAdminId))
                .singleElement().extracting(AdminRefreshToken::getRevokedAt).isNotNull();
        assertThat(tokens).filteredOn(token -> token.getAdminUserId().equals(otherAdminId))
                .singleElement().extracting(AdminRefreshToken::getRevokedAt).isNull();
        assertThat(audit.getActorAdminId()).isNull();
        assertThat(audit.getTargetAdminId()).isEqualTo(targetAdminId);
        assertThat(audit.getOutcome().name()).isEqualTo("SUCCESS");
    }

    @Test
    void refreshRevocationFailureRollsBackHashAndAuditInPostgreSql() {
        refreshTokenService.issue(new AdminPrincipal(targetAdminId));
        List<AdminRefreshToken> tokensBefore = refreshTokenRepository.findAll();
        installRefreshRevocationFailure();

        assertThatThrownBy(() -> resetService.reset(
                targetEmail, "new-admin-password".toCharArray()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageNotContaining("new-admin-password");

        assertUnchangedAfterRollback(tokensBefore);
    }

    @Test
    void auditFailureRollsBackHashAndRefreshRevocationInPostgreSql() {
        refreshTokenService.issue(new AdminPrincipal(targetAdminId));
        List<AdminRefreshToken> tokensBefore = refreshTokenRepository.findAll();
        installAuditFailure();

        assertThatThrownBy(() -> resetService.reset(
                targetEmail, "new-admin-password".toCharArray()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageNotContaining("new-admin-password");

        assertUnchangedAfterRollback(tokensBefore);
    }

    @Test
    void missingAdminDoesNotChangeOtherAdminsInvestorsOrAudit() {
        assertThatThrownBy(() -> resetService.reset(
                "missing-admin@example.com", "new-admin-password".toCharArray()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(adminUserRepository.findById(targetAdminId).orElseThrow().passwordHash())
                .isEqualTo(targetOldHash);
        assertThat(adminUserRepository.findById(otherAdminId).orElseThrow().passwordHash())
                .isEqualTo(otherAdminHash);
        assertThat(accountRepository.findById(investorId).orElseThrow().getPasswordHash())
                .isEqualTo(investorHash);
        assertThat(refreshTokenRepository.count()).isZero();
        assertThat(auditRepository.count()).isZero();
    }

    @Test
    void inactiveAdminIsNotReactivatedAndOtherDataRemainsUnchanged() {
        AdminUser target = adminUserRepository.findById(targetAdminId).orElseThrow();
        target.inactivate(INACTIVATED_AT);
        adminUserRepository.saveAndFlush(target);
        refreshTokenService.issue(new AdminPrincipal(targetAdminId));

        resetService.reset(targetEmail, "new-admin-password".toCharArray());

        AdminUser reloaded = adminUserRepository.findById(targetAdminId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AdminStatus.INACTIVE);
        assertThat(reloaded.getInactivatedAt().toInstant())
                .isEqualTo(INACTIVATED_AT.toInstant());
        assertThat(passwordEncoder.matches("new-admin-password", reloaded.passwordHash()))
                .isTrue();
        assertThat(adminUserRepository.findById(otherAdminId).orElseThrow().passwordHash())
                .isEqualTo(otherAdminHash);
        assertThat(accountRepository.findById(investorId).orElseThrow().getPasswordHash())
                .isEqualTo(investorHash);
    }

    private void assertUnchangedAfterRollback(List<AdminRefreshToken> tokensBefore) {
        AdminUser target = adminUserRepository.findById(targetAdminId).orElseThrow();
        List<AdminRefreshToken> tokensAfter = refreshTokenRepository.findAll();
        assertThat(target.passwordHash()).isEqualTo(targetOldHash);
        assertTokenRowsUnchanged(tokensBefore, tokensAfter);
        assertThat(tokensAfter).allMatch(token -> token.getRevokedAt() == null);
        assertThat(auditRepository.count()).isZero();
        assertThat(adminUserRepository.findById(otherAdminId).orElseThrow().passwordHash())
                .isEqualTo(otherAdminHash);
        assertThat(accountRepository.findById(investorId).orElseThrow().getPasswordHash())
                .isEqualTo(investorHash);
    }

    private void assertTokenRowsUnchanged(
            List<AdminRefreshToken> tokensBefore, List<AdminRefreshToken> tokensAfter) {
        assertThat(tokensAfter).hasSize(tokensBefore.size());
        for (AdminRefreshToken before : tokensBefore) {
            AdminRefreshToken after = tokensAfter.stream()
                    .filter(token -> token.getId().equals(before.getId()))
                    .findFirst().orElseThrow();
            assertThat(after.getTokenHash()).isEqualTo(before.getTokenHash());
            assertThat(after.getAdminUserId()).isEqualTo(before.getAdminUserId());
            assertThat(after.getFamilyId()).isEqualTo(before.getFamilyId());
            assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());
            assertThat(after.getExpiresAt()).isEqualTo(before.getExpiresAt());
            assertThat(after.getReplacedById()).isEqualTo(before.getReplacedById());
        }
    }

    private void installRefreshRevocationFailure() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION fail_admin_password_reset_refresh()
                RETURNS trigger AS $$
                BEGIN
                    IF NEW.revoked_at IS DISTINCT FROM OLD.revoked_at
                       AND NEW.revoked_at IS NOT NULL THEN
                        RAISE EXCEPTION 'forced refresh revocation failure';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER trg_fail_admin_password_reset_refresh
                BEFORE UPDATE OF revoked_at ON admin_refresh_token
                FOR EACH ROW EXECUTE FUNCTION fail_admin_password_reset_refresh()
                """);
    }

    private void installAuditFailure() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION fail_admin_password_reset_audit()
                RETURNS trigger AS $$
                BEGIN
                    RAISE EXCEPTION 'forced audit failure';
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER trg_fail_admin_password_reset_audit
                BEFORE INSERT ON admin_audit_event
                FOR EACH ROW EXECUTE FUNCTION fail_admin_password_reset_audit()
                """);
    }

    private void removeFailureTriggers() {
        jdbcTemplate.execute("""
                DROP TRIGGER IF EXISTS trg_fail_admin_password_reset_refresh
                ON admin_refresh_token
                """);
        jdbcTemplate.execute("""
                DROP TRIGGER IF EXISTS trg_fail_admin_password_reset_audit
                ON admin_audit_event
                """);
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fail_admin_password_reset_refresh()");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fail_admin_password_reset_audit()");
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
