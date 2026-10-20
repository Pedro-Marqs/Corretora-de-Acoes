package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
import org.junit.jupiter.api.Test;

class FinancialHistoryServiceValidationTests {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-27T09:00:00-03:00");
    private final MovementRepository movements = mock(MovementRepository.class);
    private final PatrimonialPointRepository points = mock(PatrimonialPointRepository.class);
    private final PatrimonyCalculator calculator = mock(PatrimonyCalculator.class);
    private final FinancialHistoryService service = new FinancialHistoryService(
            movements, points, calculator, Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
    private final Account account = Account.create(UUID.randomUUID(), "Investor", "12345678901",
            "history-validation@example.com", "hash", new BigDecimal("1000.00"), NOW);

    @Test
    void rejectsNullAccountFactoryInstantAndInconsistentMovement() {
        assertThatThrownBy(() -> service.record(null, NOW,
                (id, owner, occurredAt) -> Movement.initialBalance(id, owner, amount("100"), occurredAt)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.record(account, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.record(account, null,
                (id, owner, occurredAt) -> Movement.initialBalance(id, owner, amount("100"), occurredAt)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.record(account, NOW, (id, owner, occurredAt) -> null))
                .isInstanceOf(IllegalArgumentException.class);

        Account other = Account.create(UUID.randomUUID(), "Other", "52998224725",
                "other-history@example.com", "hash", amount("1000"), NOW);
        assertThatThrownBy(() -> service.record(account, NOW,
                (id, owner, occurredAt) -> Movement.initialBalance(id, other, amount("100"), occurredAt)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.record(account, NOW,
                (id, owner, occurredAt) -> Movement.initialBalance(id, owner, amount("100"), NOW.plusDays(1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
