package com.projeto.gestao.service;

import java.util.Locale;
import java.util.UUID;

import com.projeto.gestao.api.controller.AccountDetailsResponse;
import com.projeto.gestao.api.controller.ChangeEmailRequest;
import com.projeto.gestao.api.controller.ChangePasswordRequest;
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

    public AccountManagementService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            SessionRevocationRepository sessionRevocationRepository) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRevocationRepository = sessionRevocationRepository;
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
