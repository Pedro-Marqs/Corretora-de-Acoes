package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.RefreshTokenFamilyRevokedException;
import com.projeto.gestao.domain.model.AdminUser;
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
class AdminRefreshTokenPostgreSqlConcurrencyTests {
    private static final long TIMEOUT_SECONDS = 15;
    private static final String TEST_SECRET = testSecret();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminRefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRefreshTokenService refreshTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;
    private UUID adminId;

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
        adminId = UUID.randomUUID();
        adminUserRepository.saveAndFlush(AdminUser.create(adminId,
                adminId + "@example.com", passwordEncoder.encode("Senha Admin 1!"),
                OffsetDateTime.now()));
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        refreshTokenRepository.deleteAll();
        if (adminUserRepository.existsById(adminId)) {
            adminUserRepository.deleteById(adminId);
        }
    }

    @Test
    void postgreSqlRowLockAllowsOnlyOneConcurrentAdministrativeRotation() throws Exception {
        AdminRefreshTokenService.IssuedRefreshToken issued =
                refreshTokenService.issue(new AdminPrincipal(adminId));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Future<RotationOutcome> first = executor.submit(
                () -> rotate(issued.tokenValue(), ready, release));
        Future<RotationOutcome> second = executor.submit(
                () -> rotate(issued.tokenValue(), ready, release));

        assertThat(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        List<RotationOutcome> results = List.of(
                first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        assertThat(results).extracting(RotationOutcome::accepted)
                .containsExactlyInAnyOrder(true, false);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_refresh_token WHERE admin_user_id = ?",
                Integer.class, adminId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_refresh_token WHERE revoked_at IS NOT NULL",
                Integer.class)).isEqualTo(2);
    }

    private RotationOutcome rotate(String rawToken, CountDownLatch ready, CountDownLatch release)
            throws InterruptedException {
        ready.countDown();
        if (!release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new AssertionError("a barreira PostgreSQL nao foi liberada");
        }
        try {
            AdminRefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawToken);
            return new RotationOutcome(true, rotated.tokenValue());
        } catch (AuthenticationException | RefreshTokenFamilyRevokedException exception) {
            return new RotationOutcome(false, null);
        }
    }

    private record RotationOutcome(boolean accepted, String successorToken) { }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
