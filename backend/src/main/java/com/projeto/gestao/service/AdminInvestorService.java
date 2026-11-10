package com.projeto.gestao.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.api.exception.ApiException;
import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.api.exception.ResourceNotFoundException;
import com.projeto.gestao.api.exception.ValidationException;
import com.projeto.gestao.domain.dto.AdminInvestorPage;
import com.projeto.gestao.domain.dto.AdminInvestorProjection;
import com.projeto.gestao.domain.dto.AdminInvestorResponse;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.SessionRevocationRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminInvestorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminInvestorService.class);
    private final AccountRepository accountRepository;
    private final SessionRevocationRepository sessionRevocationRepository;
    private final ObjectProvider<RefreshTokenService> refreshTokenServiceProvider;
    private final AdminAuditService auditService;
    private final Clock clock;

    public AdminInvestorService(
            AccountRepository accountRepository,
            SessionRevocationRepository sessionRevocationRepository,
            ObjectProvider<RefreshTokenService> refreshTokenServiceProvider,
            AdminAuditService auditService,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.sessionRevocationRepository = sessionRevocationRepository;
        this.refreshTokenServiceProvider = refreshTokenServiceProvider;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public AdminInvestorPage list(
            AccountStatus status, String search,
            OffsetDateTime createdFrom, OffsetDateTime createdTo,
            OffsetDateTime inactivatedFrom, OffsetDateTime inactivatedTo,
            Pageable pageable, UUID actorAdminId, UUID correlationId) {
        try {
            if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
                throw new ValidationException();
            }
            if (inactivatedFrom != null && inactivatedTo != null
                    && inactivatedFrom.isAfter(inactivatedTo)) {
                throw new ValidationException();
            }
            String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
            Page<AdminInvestorProjection> page = accountRepository.findForAdministrativeView(
                    status, normalizedSearch, createdFrom, createdTo,
                    inactivatedFrom, inactivatedTo, pageable);
            auditService.record(actorAdminId, AuditActorRole.ADMIN, AdminAuditAction.LIST_INVESTORS,
                    null, null, AdminAuditOutcome.SUCCESS, correlationId);
            return new AdminInvestorPage(
                    page.map(AdminInvestorResponse::from).getContent(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        } catch (ValidationException exception) {
            recordListAuditSafely(actorAdminId, AdminAuditOutcome.DENIED, correlationId);
            throw exception;
        } catch (RuntimeException exception) {
            recordListAuditSafely(actorAdminId, AdminAuditOutcome.FAILURE, correlationId);
            throw exception;
        }
    }

    @Transactional
    public void changeStatus(
            UUID accountId, AccountStatus requestedStatus, UUID actorAdminId, UUID correlationId) {
        Account account = null;
        try {
            account = accountRepository.findForUpdateById(accountId)
                    .orElseThrow(ResourceNotFoundException::new);
            if (account.getStatus() == requestedStatus) {
                throw ConflictException.invalidAccountStatusTransition();
            }
            AdminAuditAction action;
            if (requestedStatus == AccountStatus.INACTIVE
                    && account.getStatus() == AccountStatus.ACTIVE) {
                account.inactivate(OffsetDateTime.now(clock));
                revokeInvestorCredentials(accountId);
                action = AdminAuditAction.DEACTIVATE_INVESTOR;
            } else if (requestedStatus == AccountStatus.ACTIVE
                    && account.getStatus() == AccountStatus.INACTIVE) {
                account.reactivate();
                action = AdminAuditAction.ACTIVATE_INVESTOR;
            } else {
                throw ConflictException.invalidAccountStatusTransition();
            }
            accountRepository.saveAndFlush(account);
            auditService.record(actorAdminId, AuditActorRole.ADMIN, action,
                    accountId, null, AdminAuditOutcome.SUCCESS, correlationId);
        } catch (ApiException exception) {
            recordStatusAuditSafely(actorAdminId, account, AdminAuditOutcome.DENIED, correlationId);
            throw exception;
        } catch (RuntimeException exception) {
            recordStatusAuditSafely(actorAdminId, account, AdminAuditOutcome.FAILURE, correlationId);
            throw exception;
        }
    }

    private void recordListAuditSafely(
            UUID actorAdminId, AdminAuditOutcome outcome, UUID correlationId) {
        try {
            auditService.recordInNewTransaction(actorAdminId, AuditActorRole.ADMIN,
                    AdminAuditAction.LIST_INVESTORS, null, null, outcome, correlationId);
        } catch (RuntimeException auditException) {
            LOGGER.error("Could not persist administrative investor list audit", auditException);
        }
    }

    private void recordStatusAuditSafely(
            UUID actorAdminId, Account account, AdminAuditOutcome outcome, UUID correlationId) {
        try {
            auditService.recordInNewTransaction(actorAdminId, AuditActorRole.ADMIN,
                    AdminAuditAction.ACCESS_DENIED, account == null ? null : account.getId(),
                    null, outcome, correlationId);
        } catch (RuntimeException auditException) {
            LOGGER.error("Could not persist administrative investor status audit", auditException);
        }
    }

    private void revokeInvestorCredentials(UUID accountId) {
        sessionRevocationRepository.revokeAll(accountId);
        RefreshTokenService refreshTokenService = refreshTokenServiceProvider.getIfAvailable();
        if (refreshTokenService != null) {
            refreshTokenService.revokeAllForAccount(accountId);
        }
    }
}
