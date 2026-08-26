package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WalletServiceTests {
    @Autowired private WalletService walletService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private MovementRepository movementRepository;
    @Autowired private PatrimonialPointRepository patrimonialPointRepository;

    private Account account;

    @BeforeEach
    void createAccount() {
        account = accountRepository.saveAndFlush(Account.create(UUID.randomUUID(), "Test",
                "52998224725", "wallet@example.com", "hash", new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-08-26T10:00:00-03:00")));
    }

    @Test
    void returnsBalanceAndDepositsWithoutBrokerUsingResultingPatrimonialState() {
        assertThat(walletService.balance(account.getId())).isEqualByComparingTo("10000.00");

        BigDecimal result = walletService.deposit(account.getId(), new BigDecimal("500.005"));

        assertThat(result).isEqualByComparingTo("10500.01");
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10500.01");
        assertThat(movementRepository.count()).isEqualTo(1);
        assertThat(patrimonialPointRepository.count()).isEqualTo(1);
        var movement = movementRepository.findAll().get(0);
        var point = patrimonialPointRepository.findAll().get(0);
        assertThat(movement.getMovementType()).isEqualTo(MovementType.DEPOSIT);
        assertThat(movement.getTotalAmount()).isEqualByComparingTo("500.01");
        assertThat(movement.getRemainingBalance()).isEqualByComparingTo("10500.01");
        assertThat(movement.getRealizedResult()).isNull();
        assertThat(point.getBalanceBrl()).isEqualByComparingTo("10500.01");
        assertThat(point.getPositionsValueBrl()).isEqualByComparingTo("0.00");
        assertThat(point.getPatrimonyBrl()).isEqualByComparingTo("10500.01");
    }

    @Test
    void rejectsMissingZeroNegativeAndBelowMinimumBeforeChangingState() {
        for (BigDecimal amount : new BigDecimal[] {
                BigDecimal.ZERO, new BigDecimal("-10.00"), new BigDecimal("9.999")}) {
            assertThatThrownBy(() -> walletService.deposit(account.getId(), amount))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> walletService.deposit(account.getId(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(movementRepository.count()).isZero();
        assertThat(patrimonialPointRepository.count()).isZero();
    }
}
