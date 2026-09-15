package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.Movement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

@SpringBootTest
@ActiveProfiles("test")
class FinancialHistoryServiceTests {
    @Autowired private FinancialHistoryService historyService;

    @Test
    void rejectsInvocationWithoutCallerTransaction() {
        Account account = Account.create(UUID.randomUUID(), "Test", "52998224725",
                "test@example.com", "hash", new BigDecimal("10000.00"), OffsetDateTime.now());

        assertThatThrownBy(() -> historyService.record(account,
                (id, owner, occurredAt) -> Movement.initialBalance(
                        id, owner, new BigDecimal("10000.00"), occurredAt)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
