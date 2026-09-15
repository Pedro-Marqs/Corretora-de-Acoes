package com.projeto.gestao.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.projeto.gestao.api.controller.AccountDetailsResponse;
import com.projeto.gestao.api.controller.ChangeEmailRequest;
import com.projeto.gestao.api.controller.ChangePasswordRequest;
import com.projeto.gestao.api.controller.DeleteAccountRequest;
import com.projeto.gestao.api.controller.ReactivationCheckResponse;
import com.projeto.gestao.api.controller.ReactivationRequest;
import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.SessionRevocationRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountManagementService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRevocationRepository sessionRevocationRepository;
    private final Clock clock;

    public AccountManagementService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            SessionRevocationRepository sessionRevocationRepository,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRevocationRepository = sessionRevocationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountDetailsResponse details(UUID accountId) {
        Account account = activeAccount(accountId);
        return new AccountDetailsResponse(account.getName(), maskCpf(account.getCpf()), maskEmail(account.getEmail()));
    }

    @Transactional
    public void changeEmail(UUID accountId, ChangeEmailRequest request) {
        Account account = activeAccount(accountId);
        verifyCurrentPassword(request.currentPassword(), account);
        String newEmail = request.newEmail().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByEmailIgnoreCaseAndStatusAndIdNot(
                newEmail, AccountStatus.ACTIVE, accountId)) {
            throw ConflictException.activeAccountAlreadyExists();
        }
        account.changeEmail(newEmail);
        try {
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw ConflictException.activeAccountAlreadyExists();
        }
        revokeAllSessions(accountId);
    }

    @Transactional
    public void changePassword(UUID accountId, ChangePasswordRequest request) {
        Account account = activeAccount(accountId);
        verifyCurrentPassword(request.currentPassword(), account);
        account.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        accountRepository.saveAndFlush(account);
        revokeAllSessions(accountId);
    }

    @Transactional
    public void inactivate(UUID accountId, DeleteAccountRequest request) {
        Account account = activeAccount(accountId);
        if (!account.getEmail().equalsIgnoreCase(request.email())
                || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new AuthenticationException();
        }
        account.inactivate(OffsetDateTime.now(clock));
        accountRepository.saveAndFlush(account);
        revokeAllSessions(accountId);
    }

    @Transactional(readOnly = true)
    public ReactivationCheckResponse checkReactivation(ReactivationRequest request) {
        reactivationCandidate(normalizeCpf(request.cpf()));
        return new ReactivationCheckResponse(true);
    }

    @Transactional
    public void reactivate(ReactivationRequest request) {
        Account account = reactivationCandidate(normalizeCpf(request.cpf()));
        account.reactivate();
        try {
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw ConflictException.reactivationUnavailable();
        }
    }

    private Account reactivationCandidate(String cpf) {
        if (accountRepository.existsByCpfAndStatus(cpf, AccountStatus.ACTIVE)) {
            throw ConflictException.reactivationUnavailable();
        }
        List<Account> candidates = accountRepository.findAllByCpfAndStatus(cpf, AccountStatus.INACTIVE);
        if (candidates.size() != 1) {
            throw ConflictException.reactivationUnavailable();
        }
        Account candidate = candidates.get(0);
        if (accountRepository.existsByEmailIgnoreCaseAndStatus(candidate.getEmail(), AccountStatus.ACTIVE)) {
            throw ConflictException.reactivationUnavailable();
        }
        return candidate;
    }

    private String normalizeCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }

    private Account activeAccount(UUID accountId) {
        return accountRepository.findByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
    }

    private void verifyCurrentPassword(String currentPassword, Account account) {
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new AuthenticationException();
        }
    }

    private void revokeAllSessions(UUID accountId) {
        sessionRevocationRepository.revokeAll(accountId);
    }

    private String maskCpf(String cpf) {
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }

    private String maskEmail(String email) {
        int separator = email.indexOf('@');
        return email.substring(0, 1) + "***" + email.substring(separator);
    }
}
