package com.projeto.gestao.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AccountBalanceTests {
    @Test
    void creditsPositiveAmountWithTwoDecimalsAndHalfUp() {
        Account account = account();

        account.credit(new BigDecimal("10.005"));

        assertThat(account.getBalance()).isEqualByComparingTo("10010.01");
        assertThat(account.getBalance().scale()).isEqualTo(2);
    }

    @Test
    void rejectsMissingZeroNegativeAndAmountThatRoundsToZeroWithoutChangingBalance() {
        Account account = account();

        for (BigDecimal amount : new BigDecimal[] {
                BigDecimal.ZERO, new BigDecimal("-1.00"), new BigDecimal("0.004")}) {
            assertThatThrownBy(() -> account.credit(amount))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(account.getBalance()).isEqualByComparingTo("10000.00");
        }
        assertThatThrownBy(() -> account.credit(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("10000.00");
    }

    private Account account() {
        return Account.create(UUID.randomUUID(), "Test", "52998224725", "test@example.com",
                "hash", new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-08-26T10:00:00-03:00"));
    }
}
