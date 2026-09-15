package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
class WalletServiceRollbackTests {
    @Autowired private WalletService walletService;
    @Autowired private AccountRepository accountRepository;
    @MockitoSpyBean private MovementRepository movementRepository;
    @MockitoSpyBean private PatrimonialPointRepository patrimonialPointRepository;

    private UUID accountId;

    @BeforeEach
    void createAccount() {
        patrimonialPointRepository.deleteAll();
        movementRepository.deleteAll();
        accountRepository.deleteAll();
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Test", "52998224725",
                "rollback-wallet@example.com", "hash", new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-08-26T10:00:00-03:00")));
    }

    @AfterEach
    void cleanup() {
        patrimonialPointRepository.deleteAll();
        movementRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void rollsBackBalanceWhenMovementRegistrationFails() {
        doThrow(new IllegalStateException("simulated movement failure"))
                .when(movementRepository).save(any());

        assertThatThrownBy(() -> walletService.deposit(accountId, new BigDecimal("500.00")))
                .isInstanceOf(IllegalStateException.class);

        assertUnchanged();
    }

    @Test
    void rollsBackBalanceAndMovementWhenPatrimonialPointFails() {
        doThrow(new IllegalStateException("simulated point failure"))
                .when(patrimonialPointRepository).save(any());

        assertThatThrownBy(() -> walletService.deposit(accountId, new BigDecimal("500.00")))
                .isInstanceOf(IllegalStateException.class);

        assertUnchanged();
    }

    private void assertUnchanged() {
        assertThat(accountRepository.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
        assertThat(movementRepository.count()).isZero();
        assertThat(patrimonialPointRepository.count()).isZero();
    }
}
