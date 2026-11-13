package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminUserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AdminBootstrapPostgreSqlConcurrencyTests {
    private static final String ADVISORY_LOCK_KEY = "corretora.admin.bootstrap";
    private static final long TIMEOUT_SECONDS = 15;

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private AdminBootstrapService bootstrapService;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminAuditEventRepository auditEventRepository;
    @Autowired private DataSource dataSource;

    private ExecutorService executor;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
    }

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        adminUserRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        auditEventRepository.deleteAll();
        adminUserRepository.deleteAll();
    }

    @Test
    void independentPostgreSqlConnectionsCreateOnlyOneFirstAdministrator() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        List<Future<AdminBootstrapService.BootstrapResult>> futures = new ArrayList<>();

        futures.add(submitBootstrap("first-process@example.com", ready, release));
        futures.add(submitBootstrap("second-process@example.com", ready, release));

        assertThat(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        release.countDown();

        List<AdminBootstrapService.BootstrapResult> results = List.of(
                futures.get(0).get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                futures.get(1).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        assertThat(results.stream().filter(AdminBootstrapService.BootstrapResult::created).count())
                .isEqualTo(1);
        assertThat(adminUserRepository.count()).isEqualTo(1);
        assertThat(adminUserRepository.findAll().get(0).getEmail())
                .isIn("first-process@example.com", "second-process@example.com");
    }

    @Test
    void PostgreSqlAdvisoryLockIsHeldUntilTheBootstrapTransactionCompletes() throws Exception {
        try (Connection control = dataSource.getConnection()) {
            control.setAutoCommit(false);
            try (PreparedStatement statement = control.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtext(?))")) {
                statement.setString(1, ADVISORY_LOCK_KEY);
                statement.execute();
            }

            Future<AdminBootstrapService.BootstrapResult> future =
                    executor.submit(() -> bootstrapService.bootstrap(
                            "blocked-until-commit@example.com", "SenhaAdmin1!"));
            try {
                future.get(500, TimeUnit.MILLISECONDS);
                fail("O bootstrap deveria aguardar o lock transacional PostgreSQL");
            } catch (TimeoutException expected) {
                // The external connection still owns the advisory transaction lock.
            }

            control.commit();
            assertThat(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).created()).isTrue();
        }
    }

    private Future<AdminBootstrapService.BootstrapResult> submitBootstrap(
            String email, CountDownLatch ready, CountDownLatch release) {
        return executor.submit(() -> {
            ready.countDown();
            if (!release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError("a barreira de bootstrap não foi liberada");
            }
            return bootstrapService.bootstrap(email, "SenhaAdmin1!");
        });
    }
}
