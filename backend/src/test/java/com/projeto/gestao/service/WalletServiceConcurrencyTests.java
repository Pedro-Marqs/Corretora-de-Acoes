package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WalletServiceConcurrencyTests {
    @Autowired private WalletService walletService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private MovementRepository movementRepository;
    @Autowired private PatrimonialPointRepository patrimonialPointRepository;

    private UUID accountId;

    @BeforeEach
    void createAccount() {
        patrimonialPointRepository.deleteAll();
        movementRepository.deleteAll();
        accountRepository.deleteAll();
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Test", "52998224725",
                "concurrent-wallet@example.com", "hash", new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-08-26T10:00:00-03:00")));
    }

    @AfterEach
    void cleanup() {
        patrimonialPointRepository.deleteAll();
        movementRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void serializesConcurrentDepositsWithoutLosingBalanceOrHistory() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<BigDecimal> first = depositAfter(start, "100.00");
        CompletableFuture<BigDecimal> second = depositAfter(start, "250.00");

        start.countDown();
        CompletableFuture.allOf(first, second).get(10, TimeUnit.SECONDS);

        assertThat(accountRepository.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("10350.00");
        assertThat(movementRepository.count()).isEqualTo(2);
        assertThat(patrimonialPointRepository.count()).isEqualTo(2);
    }

    private CompletableFuture<BigDecimal> depositAfter(CountDownLatch start, String amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent deposit did not start in time");
                }
                return walletService.deposit(accountId, new BigDecimal(amount));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Concurrent deposit was interrupted", exception);
            }
        });
    }
}
