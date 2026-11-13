package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminRefreshTokenRepository;
import com.projeto.gestao.repository.AdminUserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class AdminUserPostgreSqlConcurrencyTests {
    private static final String LOCK_KEY = "corretora.admin.lifecycle";
    private static final long TIMEOUT_SECONDS = 15;
    private static final String TEST_SECRET = testSecret();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminRefreshTokenRepository refreshTokenRepository;
    @Autowired private AdminAuditEventRepository auditRepository;
    @Autowired private AdminUserService adminUserService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DataSource dataSource;

    private ExecutorService executor;
    private UUID firstAdminId;
    private UUID secondAdminId;

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
        auditRepository.deleteAll();
        adminUserRepository.deleteAll();
        firstAdminId = UUID.randomUUID();
        secondAdminId = UUID.randomUUID();
        adminUserRepository.saveAndFlush(admin(firstAdminId));
        adminUserRepository.saveAndFlush(admin(secondAdminId));
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        refreshTokenRepository.deleteAll();
        auditRepository.deleteAll();
        adminUserRepository.deleteAll();
    }

    @Test
    void concurrentDeactivationOfTwoAdministratorsLeavesOneActive() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Future<Outcome> first = executor.submit(() -> deactivate(
                firstAdminId, secondAdminId, ready, release));
        Future<Outcome> second = executor.submit(() -> deactivate(
                secondAdminId, firstAdminId, ready, release));

        assertThat(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        List<Outcome> outcomes = List.of(
                first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        assertThat(outcomes).extracting(Outcome::accepted)
                .containsExactlyInAnyOrder(true, false);
        assertThat(adminUserRepository.countByStatus(AdminStatus.ACTIVE)).isEqualTo(1);
        assertThat(adminUserRepository.countByStatus(AdminStatus.INACTIVE)).isEqualTo(1);
        assertThat(auditRepository.findAll()).extracting(event -> event.getOutcome())
                .contains(AdminAuditOutcome.SUCCESS, AdminAuditOutcome.DENIED);
    }

    @Test
    void postgreSqlAdvisoryLockIsHeldUntilLifecycleTransactionCompletes() throws Exception {
        try (Connection control = dataSource.getConnection()) {
            control.setAutoCommit(false);
            try (PreparedStatement statement = control.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtext(?))")) {
                statement.setString(1, LOCK_KEY);
                statement.execute();
            }

            Future<Outcome> future = executor.submit(() -> {
                try {
                    adminUserService.changeStatus(firstAdminId, AdminStatus.INACTIVE,
                            secondAdminId, UUID.randomUUID());
                    return new Outcome(true);
                } catch (ConflictException exception) {
                    return new Outcome(false);
                }
            });
            Thread.sleep(500);
            assertThat(future.isDone()).isFalse();
            control.commit();
            assertThat(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).accepted()).isTrue();
        }
    }

    private Outcome deactivate(UUID targetId, UUID actorId,
            CountDownLatch ready, CountDownLatch release)
            throws Exception {
        ready.countDown();
        if (!release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new AssertionError("PostgreSQL concurrency barrier was not released");
        }
        try {
            adminUserService.changeStatus(targetId, AdminStatus.INACTIVE,
                    actorId, UUID.randomUUID());
            return new Outcome(true);
        } catch (ConflictException exception) {
            return new Outcome(false);
        }
    }

    private AdminUser admin(UUID id) {
        return AdminUser.create(id, id + "@example.com", passwordEncoder.encode("Senha Admin 1!"),
                OffsetDateTime.now());
    }

    private record Outcome(boolean accepted) { }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
