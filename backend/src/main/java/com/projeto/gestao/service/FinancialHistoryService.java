package com.projeto.gestao.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.PatrimonialPoint;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialHistoryService {
    private final MovementRepository movementRepository;
    private final PatrimonialPointRepository patrimonialPointRepository;
    private final PatrimonyCalculator patrimonyCalculator;
    private final Clock clock;

    public FinancialHistoryService(MovementRepository movementRepository,
            PatrimonialPointRepository patrimonialPointRepository,
            PatrimonyCalculator patrimonyCalculator, Clock clock) {
        this.movementRepository = movementRepository;
        this.patrimonialPointRepository = patrimonialPointRepository;
        this.patrimonyCalculator = patrimonyCalculator;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Movement record(Account account, MovementFactory movementFactory) {
        return record(account, OffsetDateTime.now(clock), movementFactory);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Movement record(Account account, OffsetDateTime occurredAt,
            MovementFactory movementFactory) {
        if (account == null || movementFactory == null) {
            throw new IllegalArgumentException("Account and movement factory are required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Movement instant is required");
        }
        Movement movement = movementFactory.create(UUID.randomUUID(), account, occurredAt);
        if (movement == null || movement.getAccount() != account
                || !occurredAt.equals(movement.getOccurredAt())) {
            throw new IllegalArgumentException("Movement must use the supplied account and instant");
        }
        movementRepository.save(movement);

        PatrimonyCalculation calculation = patrimonyCalculator.calculate(account);
        PatrimonialPoint point = PatrimonialPoint.create(UUID.randomUUID(), account, movement,
                calculation.balanceBrl(), calculation.positionsValueBrl(), calculation.usdBrlRate(),
                calculation.patrimonyBrl(), occurredAt);
        patrimonialPointRepository.save(point);
        return movement;
    }

    @FunctionalInterface
    public interface MovementFactory {
        Movement create(UUID id, Account account, OffsetDateTime occurredAt);
    }
}
