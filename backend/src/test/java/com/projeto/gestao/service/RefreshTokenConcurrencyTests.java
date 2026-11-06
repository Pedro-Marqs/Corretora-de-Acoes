package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenConcurrencyTests {
    private static final String TEST_SECRET = testSecret();

    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RefreshTokenService refreshTokenService;

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
        executor.shutdownNow();
    }

    @Test
    void concurrentRotationAllowsOnlyOneUseOfTheSameRefreshToken() throws Exception {
        RefreshTokenService.IssuedRefreshToken issued =
                refreshTokenService.issue(new AccountPrincipal(accountId));
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> first = executor.submit(() -> rotate(start, issued.tokenValue()));
        Future<Boolean> second = executor.submit(() -> rotate(start, issued.tokenValue()));
        start.countDown();

        assertThat(first.get()).isNotEqualTo(second.get());
    }

    private boolean rotate(CountDownLatch start, String rawToken) throws InterruptedException {
        start.await();
        try {
            refreshTokenService.rotate(rawToken);
            return true;
        } catch (AuthenticationException | RefreshTokenFamilyRevokedException exception) {
            return false;
        }
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
