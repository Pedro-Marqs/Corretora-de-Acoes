package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import com.projeto.gestao.api.controller.CreateAccountRequest;
import com.projeto.gestao.api.controller.CreateAccountResponse;
import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.repository.AccountRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountRegistrationService {
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("10000.00");

    private final AccountRepository accountRepository;
    private final FinancialHistoryService financialHistoryService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountRegistrationService(
            AccountRepository accountRepository,
            FinancialHistoryService financialHistoryService,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.financialHistoryService = financialHistoryService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public CreateAccountResponse register(CreateAccountRequest request) {
        String name = request.name().trim().replaceAll("\\s+", " ");
        String cpf = request.cpf().replaceAll("\\D", "");
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByCpfAndStatus(cpf, AccountStatus.ACTIVE)
                || accountRepository.existsByEmailIgnoreCaseAndStatus(email, AccountStatus.ACTIVE)) {
            throw ConflictException.activeAccountAlreadyExists();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        Account account = Account.create(
                UUID.randomUUID(), name, cpf, email, passwordEncoder.encode(request.password()),
                INITIAL_BALANCE, now);
        try {
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw ConflictException.activeAccountAlreadyExists();
        }
        financialHistoryService.record(account, now,
                (id, registeredAccount, occurredAt) -> Movement.initialBalance(
                        id, registeredAccount, INITIAL_BALANCE, occurredAt));
        return new CreateAccountResponse(account.getId(), account.getName(),
                account.getBalance(), account.getStatus());
    }
}
