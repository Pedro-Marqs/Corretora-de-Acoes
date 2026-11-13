package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import com.projeto.gestao.api.exception.RefreshTokenFamilyRevokedException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.RefreshTokenRepository;
import com.projeto.gestao.security.AccountPrincipal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RefreshTokenPostgreSqlConcurrencyTests {
    private static final long AWAIT_TIMEOUT_SECONDS = 15;
    private static final long POLL_INTERVAL_MILLIS = 10;
    private static final long CONTROL_ADVISORY_LOCK_KEY = 873_214_609_731L;
    private static final String TEST_LOCK_FUNCTION = "test_wait_for_refresh_lock_gate";
    private static final String TEST_LOCK_TRIGGER = "test_refresh_rotation_gate";
    private static final String TEST_ROLLBACK_CONSTRAINT = "test_refresh_rotation_rejects_successor";
    private static final String TEST_SECRET = testSecret();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @MockitoSpyBean private RefreshTokenRepository refreshTokenRepository;

    private ExecutorService executor;
    private UUID accountId;
    private Connection controlLockConnection;
    private long controlLockPid;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("spring.datasource.hikari.connection-init-sql",
                () -> "SET lock_timeout TO '5s'; SET statement_timeout TO '30s'");
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "false");
        registry.add("app.security.jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor PostgreSQL Concorrente",
                UUID.randomUUID().toString().replace("-", "").substring(0, 11),
                accountId + "@example.com", passwordEncoder.encode("Senha PostgreSQL 1!"),
                new BigDecimal("10000.00"), OffsetDateTime.now()));
        createLockGate();
        acquireControlLock();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws Throwable {
        Throwable cleanupFailure = null;
        cleanupFailure = runCleanup(cleanupFailure, "liberar o gate de lock", this::openLockGateIfCreated);
        cleanupFailure = runCleanup(cleanupFailure, "encerrar o executor", this::shutdownExecutor);
        cleanupFailure = runCleanup(cleanupFailure, "remover a conta de teste", this::deleteTestAccount);
        cleanupFailure = runCleanup(cleanupFailure, "remover a constraint de rollback", this::dropRollbackConstraint);
        cleanupFailure = runCleanup(cleanupFailure, "remover o gate PostgreSQL", this::dropLockGate);
        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
    }

    @Test
    void concurrentRotationUsesPostgreSqlPessimisticLockAndRevokesFamilyOnReuse() throws Throwable {
        RefreshTokenService.IssuedRefreshToken issued =
                refreshTokenService.issue(new AccountPrincipal(accountId));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Future<RotationOutcome> first = null;
        Future<RotationOutcome> second = null;

        Throwable primaryFailure = null;
        try {
            first = executor.submit(() -> rotate(ready, release, issued.tokenValue()));
            second = executor.submit(() -> rotate(ready, release, issued.tokenValue()));

            assertThat(ready.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .as("as duas threads PostgreSQL devem chegar a barreira de prontidao")
                    .isTrue();
            release.countDown();
            LockEvidence evidence = awaitPostgreSqlBlockingLocks();
            assertThat(evidence.controlObservation())
                    .as("a primeira rotação deve aguardar o advisory lock da conexão de controle: %s", evidence)
                    .isNotNull();
            assertThat(evidence.rotationObservation())
                    .as("a segunda rotação deve aguardar o lock da primeira rotação: %s", evidence)
                    .isNotNull();
            assertLockObservation(evidence.controlObservation());
            assertLockObservation(evidence.rotationObservation());
            assertThat(evidence.controlObservation().blockerPid())
                    .as("o bloqueador deve ser a conexão de controle")
                    .isEqualTo(controlLockPid);
            assertThat(evidence.controlObservation().blockerQuery())
                    .containsIgnoringCase("pg_advisory_xact_lock");
            assertThat(evidence.controlObservation().blockerState())
                    .containsIgnoringCase("idle in transaction");
            assertThat(evidence.rotationObservation().blockerPid())
                    .as("a segunda rotação deve ser bloqueada pela primeira")
                    .isEqualTo(evidence.controlObservation().blockedPid());
            assertThat(evidence.rotationObservation().blockerQuery())
                    .containsIgnoringCase("refresh_token");
            assertThat(evidence.controlObservation().blockedPid())
                    .isNotEqualTo(evidence.rotationObservation().blockedPid());
            openLockGate();

            List<RotationOutcome> results = List.of(
                    getWithTimeout(first), getWithTimeout(second));
            assertThat(results).extracting(RotationOutcome::accepted)
                    .containsExactlyInAnyOrder(true, false);
            RotationOutcome accepted = results.stream()
                    .filter(RotationOutcome::accepted)
                    .findFirst()
                    .orElseThrow();
            RotationOutcome rejected = results.stream()
                    .filter(outcome -> !outcome.accepted())
                    .findFirst()
                    .orElseThrow();
            assertThat(accepted.successorToken()).isNotBlank();
            assertThat(rejected.familyRevoked()).isTrue();

            assertSuccessorIntegrity(accepted.successorToken());
        } catch (Throwable failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            release.countDown();
            Future<RotationOutcome> firstFuture = first;
            Future<RotationOutcome> secondFuture = second;
            Throwable cleanupFailure = null;
            cleanupFailure = runCleanup(cleanupFailure, "liberar o gate de lock", this::openLockGateIfCreated);
            cleanupFailure = runCleanup(cleanupFailure, "cancelar a primeira tarefa", () -> cancelIfPending(firstFuture));
            cleanupFailure = runCleanup(cleanupFailure, "cancelar a segunda tarefa", () -> cancelIfPending(secondFuture));
            cleanupFailure = runCleanup(cleanupFailure, "encerrar o executor", this::shutdownExecutor);
            if (cleanupFailure != null) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }

    @Test
    void rotationFailureRollsBackOriginalTokenOnPostgreSql() {
        openLockGate();
        RefreshTokenService.IssuedRefreshToken issued =
                refreshTokenService.issue(new AccountPrincipal(accountId));
        UUID originalId = jdbcTemplate.queryForObject(
                "SELECT id FROM refresh_token WHERE account_id = ?", UUID.class, accountId);
        String originalHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_token WHERE id = ?", String.class, originalId);
        UUID originalFamilyId = jdbcTemplate.queryForObject(
                "SELECT family_id FROM refresh_token WHERE id = ?", UUID.class, originalId);

        doThrow(new IllegalStateException("falha de flush simulada"))
                .when(refreshTokenRepository).flush();

        assertThatThrownBy(() -> refreshTokenService.rotate(issued.tokenValue()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("falha de flush simulada");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE account_id = ?", Integer.class, accountId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM refresh_token WHERE id = ?", OffsetDateTime.class, originalId))
                .isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT replaced_by_id FROM refresh_token WHERE id = ?", UUID.class, originalId))
                .isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_token WHERE id = ?", String.class, originalId))
                .isEqualTo(originalHash);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT family_id FROM refresh_token WHERE id = ?", UUID.class, originalId))
                .isEqualTo(originalFamilyId);
    }

    @Test
    void realPostgreSqlConstraintFailureRollsBackRotation() throws Throwable {
        openLockGate();
        RefreshTokenService.IssuedRefreshToken issued =
                refreshTokenService.issue(new AccountPrincipal(accountId));
        UUID originalId = jdbcTemplate.queryForObject(
                "SELECT id FROM refresh_token WHERE account_id = ?", UUID.class, accountId);
        String originalHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_token WHERE id = ?", String.class, originalId);
        UUID originalFamilyId = jdbcTemplate.queryForObject(
                "SELECT family_id FROM refresh_token WHERE id = ?", UUID.class, originalId);

        Throwable primaryFailure = null;
        try {
            jdbcTemplate.execute("ALTER TABLE refresh_token ADD CONSTRAINT " + TEST_ROLLBACK_CONSTRAINT
                    + " CHECK (replaced_by_id IS NULL)");

            Throwable failure = catchThrowable(() -> refreshTokenService.rotate(issued.tokenValue()));

            assertThat(failure).isInstanceOf(DataIntegrityViolationException.class);
            org.hibernate.exception.ConstraintViolationException databaseFailure = findCause(
                    failure, org.hibernate.exception.ConstraintViolationException.class);
            assertThat(databaseFailure).isNotNull();
            assertThat(databaseFailure.getSQLState()).isEqualTo("23514");
            assertThat(databaseFailure.getConstraintName()).isEqualTo(TEST_ROLLBACK_CONSTRAINT);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM refresh_token WHERE account_id = ?", Integer.class, accountId))
                    .isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT revoked_at FROM refresh_token WHERE id = ?", OffsetDateTime.class, originalId))
                    .isNull();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT replaced_by_id FROM refresh_token WHERE id = ?", UUID.class, originalId))
                    .isNull();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT token_hash FROM refresh_token WHERE id = ?", String.class, originalId))
                    .isEqualTo(originalHash);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT family_id FROM refresh_token WHERE id = ?", UUID.class, originalId))
                    .isEqualTo(originalFamilyId);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM refresh_token WHERE account_id = ? AND replaced_by_id IS NOT NULL",
                    Integer.class, accountId)).isZero();
        } catch (Throwable failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            try {
                dropRollbackConstraint();
            } catch (Throwable cleanupFailure) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }

    private RotationOutcome rotate(
            CountDownLatch ready, CountDownLatch release, String rawToken) throws InterruptedException {
        ready.countDown();
        if (!release.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new AssertionError("a barreira PostgreSQL nao foi liberada no prazo");
        }
        try {
            RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawToken);
            return new RotationOutcome(true, rotated.tokenValue(), false);
        } catch (RefreshTokenFamilyRevokedException exception) {
            return new RotationOutcome(false, null, true);
        }
    }

    private RotationOutcome getWithTimeout(Future<RotationOutcome> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void assertSuccessorIntegrity(String successorRawToken) {
        UUID originalId = jdbcTemplate.queryForObject(
                "SELECT id FROM refresh_token WHERE account_id = ? AND replaced_by_id IS NOT NULL",
                UUID.class, accountId);
        String originalHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_token WHERE id = ?", String.class, originalId);
        UUID familyId = jdbcTemplate.queryForObject(
                "SELECT family_id FROM refresh_token WHERE id = ?", UUID.class, originalId);
        UUID successorId = jdbcTemplate.queryForObject(
                "SELECT replaced_by_id FROM refresh_token WHERE id = ?", UUID.class, originalId);

        assertThat(successorId).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE account_id = ? AND replaced_by_id IS NOT NULL",
                Integer.class, accountId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE account_id = ?", Integer.class, accountId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE id = ? AND account_id = ? "
                        + "AND family_id = ? AND revoked_at IS NOT NULL AND replaced_by_id IS NULL",
                Integer.class, successorId, accountId, familyId)).isEqualTo(1);
        String successorHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_token WHERE id = ?", String.class, successorId);
        assertThat(successorHash).isNotEqualTo(originalHash)
                .isEqualTo(RefreshTokenService.hash(successorRawToken));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE account_id = ? AND family_id = ? "
                        + "AND revoked_at IS NOT NULL",
                Integer.class, accountId, familyId)).isEqualTo(2);
    }

    private void createLockGate() {
        dropLockGate();
        try {
            jdbcTemplate.execute("""
                CREATE FUNCTION test_wait_for_refresh_lock_gate()
                RETURNS trigger
                LANGUAGE plpgsql
                AS $$
                BEGIN
                    PERFORM pg_advisory_xact_lock(873214609731);
                    RETURN NEW;
                END;
                $$
                """);
            jdbcTemplate.execute("""
                CREATE TRIGGER test_refresh_rotation_gate
                BEFORE UPDATE OF replaced_by_id ON refresh_token
                FOR EACH ROW
                WHEN (OLD.replaced_by_id IS NULL AND NEW.replaced_by_id IS NOT NULL)
                EXECUTE FUNCTION test_wait_for_refresh_lock_gate()
                """);
        } catch (RuntimeException failure) {
            dropLockGateAfterFailure(failure);
            throw failure;
        }
    }

    private void openLockGate() {
        if (controlLockConnection == null) {
            return;
        }
        Connection connection = controlLockConnection;
        controlLockConnection = null;
        controlLockPid = 0;
        SQLException primaryFailure = null;
        try {
            connection.commit();
        } catch (SQLException failure) {
            primaryFailure = failure;
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                primaryFailure.addSuppressed(rollbackFailure);
            }
        }
        try {
            connection.close();
        } catch (SQLException closeFailure) {
            if (primaryFailure == null) {
                primaryFailure = closeFailure;
            } else {
                primaryFailure.addSuppressed(closeFailure);
            }
        }
        if (primaryFailure != null) {
            throw new IllegalStateException("falha ao liberar o advisory lock de controle", primaryFailure);
        }
    }

    private void openLockGateIfCreated() {
        if (controlLockConnection != null) {
            openLockGate();
        }
    }

    private void dropLockGate() {
        RuntimeException failure = null;
        try {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + TEST_LOCK_TRIGGER + " ON refresh_token");
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            jdbcTemplate.execute("DROP FUNCTION IF EXISTS " + TEST_LOCK_FUNCTION + "()");
        } catch (RuntimeException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw new IllegalStateException("falha ao limpar o gate PostgreSQL de teste", failure);
        }
    }

    private void dropLockGateAfterFailure(Throwable primaryFailure) {
        try {
            dropLockGate();
        } catch (RuntimeException cleanupFailure) {
            primaryFailure.addSuppressed(cleanupFailure);
        }
    }

    private void deleteTestAccount() {
        if (accountId != null && accountRepository.existsById(accountId)) {
            accountRepository.deleteById(accountId);
        }
    }

    private Throwable runCleanup(
            Throwable aggregateFailure, String operation, Runnable cleanupAction) {
        try {
            cleanupAction.run();
        } catch (Throwable cleanupFailure) {
            Throwable reportedFailure = aggregateFailure;
            if (reportedFailure == null) {
                reportedFailure = new IllegalStateException("falha ao " + operation, cleanupFailure);
            } else {
                reportedFailure.addSuppressed(new IllegalStateException(
                        "falha ao " + operation, cleanupFailure));
            }
            return reportedFailure;
        }
        return aggregateFailure;
    }

    private void acquireControlLock() {
        try {
            controlLockConnection = dataSource.getConnection();
            controlLockConnection.setAutoCommit(false);
            try (PreparedStatement statement = controlLockConnection.prepareStatement(
                    "SELECT pg_backend_pid()")) {
                try (var resultSet = statement.executeQuery()) {
                    resultSet.next();
                    controlLockPid = resultSet.getLong(1);
                }
            }
            // Keep advisory lock acquisition as the last query visible in
            // pg_stat_activity while the service transactions are blocked.
            try (PreparedStatement statement = controlLockConnection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(?)")) {
                statement.setLong(1, CONTROL_ADVISORY_LOCK_KEY);
                statement.execute();
            }
        } catch (SQLException failure) {
            closeControlLockAfterFailure(failure);
            throw new IllegalStateException("falha ao preparar a transacao de controle PostgreSQL", failure);
        }
    }

    private LockEvidence awaitPostgreSqlBlockingLocks() throws InterruptedException {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(AWAIT_TIMEOUT_SECONDS);
        List<LockObservation> lastObservations = List.of();
        while (System.nanoTime() < deadline) {
            List<LockObservation> observations = jdbcTemplate.query("""
                    SELECT blocked.pid AS blocked_pid,
                           blocker.pid AS blocker_pid,
                           blocked.wait_event_type,
                           blocked.wait_event,
                           blocked.state AS blocked_state,
                           blocked.query AS blocked_query,
                           blocker.state AS blocker_state,
                           blocker.query AS blocker_query
                    FROM pg_stat_activity blocked
                    CROSS JOIN LATERAL unnest(pg_blocking_pids(blocked.pid)) AS blocking(blocker_pid)
                    JOIN pg_stat_activity blocker ON blocker.pid = blocking.blocker_pid
                    WHERE blocked.datname = current_database()
                      AND blocked.pid <> pg_backend_pid()
                      AND blocked.wait_event_type = 'Lock'
                      AND blocked.query ILIKE '%refresh_token%'
                    """, (resultSet, rowNum) -> new LockObservation(
                    resultSet.getLong("blocked_pid"),
                    resultSet.getLong("blocker_pid"),
                    resultSet.getString("wait_event_type"),
                    resultSet.getString("wait_event"),
                    resultSet.getString("blocked_state"),
                    resultSet.getString("blocked_query"),
                    resultSet.getString("blocker_state"),
                    resultSet.getString("blocker_query")));
            lastObservations = observations;
            LockObservation controlObservation = observations.stream()
                    .filter(observation -> observation.blockerPid() == controlLockPid)
                    .findFirst()
                    .orElse(null);
            LockObservation rotationObservation = observations.stream()
                    .filter(observation -> observation.blockerPid() != controlLockPid
                            && observation.blockerQuery() != null
                            && observation.blockerQuery().toLowerCase().contains("refresh_token"))
                    .findFirst()
                    .orElse(null);
            if (controlObservation != null && rotationObservation != null) {
                return new LockEvidence(controlObservation, rotationObservation);
            }
            // Polling only observes PostgreSQL; synchronization is provided by
            // the advisory gate and the row lock, not by this sleep.
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("PostgreSQL nao exibiu a cadeia de locks esperada; observacoes finais: "
                + lastObservations);
    }

    private void assertLockObservation(LockObservation observation) {
        assertThat(observation.blockerPid()).as("observacao=%s", observation).isPositive();
        assertThat(observation.blockedPid()).as("observacao=%s", observation).isPositive()
                .isNotEqualTo(observation.blockerPid());
        assertThat(observation.waitEventType()).as("observacao=%s", observation).isEqualTo("Lock");
        assertThat(observation.waitEvent()).as("observacao=%s", observation).isNotBlank();
        assertThat(observation.blockedState()).as("observacao=%s", observation).isEqualTo("active");
        assertThat(observation.blockedQuery()).as("observacao=%s", observation)
                .containsIgnoringCase("refresh_token");
        assertThat(observation.blockerState()).as("observacao=%s", observation).isNotBlank();
        assertThat(observation.blockerQuery()).as("observacao=%s", observation).isNotBlank();
    }

    private void dropRollbackConstraint() {
        jdbcTemplate.execute("ALTER TABLE refresh_token DROP CONSTRAINT IF EXISTS " + TEST_ROLLBACK_CONSTRAINT);
    }

    private void closeControlLockAfterFailure(Throwable primaryFailure) {
        if (controlLockConnection == null) {
            return;
        }
        Connection connection = controlLockConnection;
        controlLockConnection = null;
        try {
            connection.rollback();
        } catch (SQLException cleanupFailure) {
            primaryFailure.addSuppressed(cleanupFailure);
        } finally {
            closeConnection(connection, primaryFailure);
        }
    }

    private void closeConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException failure) {
            throw new IllegalStateException("falha ao fechar a conexao de controle PostgreSQL", failure);
        }
    }

    private void closeConnection(Connection connection, Throwable primaryFailure) {
        try {
            connection.close();
        } catch (SQLException cleanupFailure) {
            primaryFailure.addSuppressed(cleanupFailure);
        }
    }

    private static <T extends Throwable> T findCause(Throwable failure, Class<T> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private void cancelIfPending(Future<?> future) {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    private void shutdownExecutor() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            assertThat(executor.awaitTermination(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .as("o executor PostgreSQL deve ser encerrado no prazo")
                    .isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("a interrupcao impediu o encerramento do executor", exception);
        }
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private record RotationOutcome(boolean accepted, String successorToken, boolean familyRevoked) { }

    private record LockObservation(long blockedPid, long blockerPid, String waitEventType,
                                   String waitEvent, String blockedState, String blockedQuery,
                                   String blockerState, String blockerQuery) { }

    private record LockEvidence(LockObservation controlObservation, LockObservation rotationObservation) { }
}
