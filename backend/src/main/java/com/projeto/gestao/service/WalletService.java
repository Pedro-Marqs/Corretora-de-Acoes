package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    static final BigDecimal MINIMUM_DEPOSIT = new BigDecimal("10.00");

    private final AccountRepository accountRepository;
    private final FinancialHistoryService financialHistoryService;

    public WalletService(AccountRepository accountRepository,
            FinancialHistoryService financialHistoryService) {
        this.accountRepository = accountRepository;
        this.financialHistoryService = financialHistoryService;
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(UUID accountId) {
        return activeAccount(accountId).getBalance();
    }

    @Transactional
    public BigDecimal deposit(UUID accountId, BigDecimal amount) {
        BigDecimal normalizedAmount = validDeposit(amount);
        Account account = activeAccountForUpdate(accountId);
        account.credit(normalizedAmount);
        BigDecimal resultingBalance = account.getBalance();
        financialHistoryService.record(account,
                (id, owner, occurredAt) -> Movement.deposit(
                        id, owner, normalizedAmount, resultingBalance, occurredAt));
        return resultingBalance;
    }

    private BigDecimal validDeposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(MINIMUM_DEPOSIT) < 0) {
            throw new IllegalArgumentException("Deposit must be at least BRL 10.00");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private Account activeAccount(UUID accountId) {
        if (accountId == null) {
            throw new AuthenticationException();
        }
        return accountRepository.findByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
    }

    private Account activeAccountForUpdate(UUID accountId) {
        if (accountId == null) {
            throw new AuthenticationException();
        }
        return accountRepository.findForUpdateByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
    }
}
