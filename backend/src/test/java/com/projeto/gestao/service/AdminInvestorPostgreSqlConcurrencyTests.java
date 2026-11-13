package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.security.SecureRandom;
import java.util.Base64;

import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.repository.RefreshTokenRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
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
class AdminInvestorPostgreSqlConcurrencyTests {
    private static final long TIMEOUT_SECONDS = 15;
    private static final String TEST_SECRET = testSecret();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private AccountRepository accountRepository;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminAuditEventRepository auditEventRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private AdminInvestorService investorService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;
    private UUID adminId;
    private UUID accountId;

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
        refreshTokenRepository.deleteAll();
        auditEventRepository.deleteAll();
        accountRepository.deleteAll();
        adminUserRepository.deleteAll();

        adminId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        adminUserRepository.saveAndFlush(AdminUser.create(
                adminId, "admin@example.com", passwordEncoder.encode("SenhaAdmin1!"),
                OffsetDateTime.now()));
        accountRepository.saveAndFlush(Account.create(
                accountId, "Investidor PostgreSQL", "52998224725", "investor@example.com",
                passwordEncoder.encode("SenhaInvestor1!"), new BigDecimal("10000.00"),
                OffsetDateTime.now()));
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS test_admin_audit_failure ON admin_audit_event");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS test_fail_admin_audit()");
        if (executor != null) {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        refreshTokenRepository.deleteAll();
        auditEventRepository.deleteAll();
        if (accountRepository.existsById(accountId)) {
            accountRepository.deleteById(accountId);
        }
        if (adminUserRepository.existsById(adminId)) {
            adminUserRepository.deleteById(adminId);
        }
    }

    @Test
    void concurrentStatusChangesUseTheAccountLockAndOnlyOneTransitionSucceeds() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        List<Future<TransitionOutcome>> futures = new ArrayList<>();
        futures.add(submitTransition(ready, release));
        futures.add(submitTransition(ready, release));

        assertThat(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        List<TransitionOutcome> outcomes = List.of(
                futures.get(0).get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                futures.get(1).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        assertThat(outcomes).containsExactlyInAnyOrder(
                TransitionOutcome.SUCCESS, TransitionOutcome.CONFLICT);
        assertThat(accountRepository.findById(accountId).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.INACTIVE);
        assertThat(accountRepository.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
        assertThat(auditEventRepository.findAll()).extracting(event -> event.getAction())
                .contains(AdminAuditAction.DEACTIVATE_INVESTOR, AdminAuditAction.ACCESS_DENIED);
    }

    @Test
    void auditFailureRollsBackStatusAndPreservesFinancialData() {
        jdbcTemplate.execute("""
                CREATE FUNCTION test_fail_admin_audit() RETURNS trigger
                LANGUAGE plpgsql AS $$
                BEGIN
                    RAISE EXCEPTION 'forced administrative audit failure';
                END;
                $$
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER test_admin_audit_failure
                BEFORE INSERT ON admin_audit_event
                FOR EACH ROW EXECUTE FUNCTION test_fail_admin_audit()
                """);

        assertThatThrownBy(() -> investorService.changeStatus(
                accountId, AccountStatus.INACTIVE, adminId, UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class);

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getInactivatedAt()).isNull();
        assertThat(account.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(refreshTokenRepository.findAll()).isEmpty();
        assertThat(auditEventRepository.findAll()).isEmpty();
    }

    private Future<TransitionOutcome> submitTransition(
            CountDownLatch ready, CountDownLatch release) {
        return executor.submit(() -> {
            ready.countDown();
            if (!release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError("a barreira PostgreSQL nao foi liberada");
            }
            try {
                investorService.changeStatus(
                        accountId, AccountStatus.INACTIVE, adminId, UUID.randomUUID());
                return TransitionOutcome.SUCCESS;
            } catch (ConflictException exception) {
                return TransitionOutcome.CONFLICT;
            }
        });
    }

    private enum TransitionOutcome { SUCCESS, CONFLICT }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
