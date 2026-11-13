package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.RefreshTokenFamilyRevokedException;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.security.AccountPrincipal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenConcurrencyTests {
    private static final long AWAIT_TIMEOUT_SECONDS = 10;
    private static final String TEST_SECRET = testSecret();

    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;
    private UUID accountId;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "false");
        registry.add("app.security.jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor Concorrente",
                UUID.randomUUID().toString().replace("-", "").substring(0, 11),
                accountId + "@example.com", passwordEncoder.encode("Senha Concorrente 1!"),
                new BigDecimal("10000.00"), OffsetDateTime.now()));
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        try {
            shutdownExecutor();
        } finally {
            if (accountId != null && accountRepository.existsById(accountId)) {
                accountRepository.deleteById(accountId);
            }
        }
    }

    @Test
    void concurrentRotationAllowsOnlyOneUseOfTheSameRefreshToken() throws Exception {
        RefreshTokenService.IssuedRefreshToken issued =
                refreshTokenService.issue(new AccountPrincipal(accountId));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Future<RotationOutcome> first = null;
        Future<RotationOutcome> second = null;

        try {
            first = executor.submit(() -> rotate(ready, release, issued.tokenValue()));
            second = executor.submit(() -> rotate(ready, release, issued.tokenValue()));

            assertThat(ready.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .as("as duas threads devem chegar a barreira de prontidao")
                    .isTrue();
            // A barreira sincroniza o inicio das duas tarefas, mas nao prova a
            // sobreposicao de cada etapa interna no banco; o lock e validado
            // pelo resultado unico de rotacao persistido.
            release.countDown();

            List<RotationOutcome> results = List.of(
                    first.get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    second.get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

            assertThat(results).extracting(RotationOutcome::accepted)
                    .containsExactlyInAnyOrder(true, false);
            RotationOutcome accepted = results.stream()
                    .filter(RotationOutcome::accepted)
                    .findFirst()
                    .orElseThrow();
            assertThat(accepted.successorToken()).isNotBlank();
            assertSuccessorIntegrity(accepted.successorToken());
        } finally {
            release.countDown();
            cancelIfPending(first);
            cancelIfPending(second);
            shutdownExecutor();
        }
    }

    private RotationOutcome rotate(CountDownLatch ready, CountDownLatch release, String rawToken)
            throws InterruptedException {
        ready.countDown();
        if (!release.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new AssertionError("a barreira de inicio nao foi liberada no prazo");
        }
        try {
            RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawToken);
            return new RotationOutcome(true, rotated.tokenValue(), false);
        } catch (AuthenticationException | RefreshTokenFamilyRevokedException exception) {
            return new RotationOutcome(false, null,
                    exception instanceof RefreshTokenFamilyRevokedException);
        }
    }

    private void cancelIfPending(Future<?> future) {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
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

    private record RotationOutcome(boolean accepted, String successorToken, boolean familyRevoked) {
    }

    private void shutdownExecutor() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            assertThat(executor.awaitTermination(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .as("o executor deve ser encerrado no prazo")
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
}
