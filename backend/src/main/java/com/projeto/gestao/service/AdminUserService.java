package com.projeto.gestao.service;

import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import com.projeto.gestao.api.exception.ApiException;
import com.projeto.gestao.api.exception.AuthorizationException;
import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.api.exception.ResourceNotFoundException;
import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.repository.AdminUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserService.class);
    private static final String LIFECYCLE_LOCK_KEY = "corretora.admin.lifecycle";

    private final AdminUserRepository adminUserRepository;
    private final ObjectProvider<AdminRefreshTokenService> refreshTokenServiceProvider;
    private final AdminAuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public AdminUserService(
            AdminUserRepository adminUserRepository,
            ObjectProvider<AdminRefreshTokenService> refreshTokenServiceProvider,
            AdminAuditService auditService,
            JdbcTemplate jdbcTemplate,
            Clock clock) {
        this.adminUserRepository = adminUserRepository;
        this.refreshTokenServiceProvider = refreshTokenServiceProvider;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Transactional
    public void changeStatus(
            UUID targetAdminId, AdminStatus requestedStatus,
            UUID actorAdminId, UUID correlationId) {
        AdminUser target = null;
        try {
            acquireLifecycleLock();
            target = adminUserRepository.findById(targetAdminId)
                    .orElseThrow(ResourceNotFoundException::new);
            if (target.getStatus() == requestedStatus) {
                throw ConflictException.invalidAdminStatusTransition();
            }
            if (requestedStatus == AdminStatus.INACTIVE) {
                ensureAnotherActiveAdministratorExists();
                ensureActiveActor(actorAdminId);
                target.inactivate(OffsetDateTime.now(clock));
                revokeAdministrativeSessions(targetAdminId);
            } else if (requestedStatus == AdminStatus.ACTIVE) {
                ensureActiveActor(actorAdminId);
                target.reactivate();
            } else {
                throw ConflictException.invalidAdminStatusTransition();
            }
            adminUserRepository.saveAndFlush(target);
            auditService.record(actorAdminId, AuditActorRole.ADMIN,
                    actionFor(requestedStatus), null, targetAdminId,
                    AdminAuditOutcome.SUCCESS, correlationId);
        } catch (ApiException exception) {
            recordRejectedStatusSafely(actorAdminId, target, requestedStatus,
                    AdminAuditOutcome.DENIED, correlationId);
            throw exception;
        } catch (RuntimeException exception) {
            recordRejectedStatusSafely(actorAdminId, target, requestedStatus,
                    AdminAuditOutcome.FAILURE, correlationId);
            throw exception;
        }
    }

    private void ensureAnotherActiveAdministratorExists() {
        if (adminUserRepository.countByStatus(AdminStatus.ACTIVE) <= 1) {
            throw ConflictException.lastActiveAdministratorCannotBeInactivated();
        }
    }

    private void ensureActiveActor(UUID actorAdminId) {
        if (actorAdminId == null
                || adminUserRepository.findByIdAndStatus(actorAdminId, AdminStatus.ACTIVE).isEmpty()) {
            throw new AuthorizationException();
        }
    }

    private void revokeAdministrativeSessions(UUID adminId) {
        AdminRefreshTokenService refreshTokenService =
                refreshTokenServiceProvider.getIfAvailable();
        if (refreshTokenService != null) {
            refreshTokenService.revokeAllForAdmin(adminId);
        }
    }

    private AdminAuditAction actionFor(AdminStatus requestedStatus) {
        return requestedStatus == AdminStatus.ACTIVE
                ? AdminAuditAction.ACTIVATE_ADMIN : AdminAuditAction.DEACTIVATE_ADMIN;
    }

    private void recordRejectedStatusSafely(
            UUID actorAdminId, AdminUser target, AdminStatus requestedStatus,
            AdminAuditOutcome outcome, UUID correlationId) {
        try {
            auditService.recordInNewTransaction(actorAdminId, AuditActorRole.ADMIN,
                    actionFor(requestedStatus), null, target == null ? null : target.getId(),
                    outcome, correlationId);
        } catch (RuntimeException auditException) {
            LOGGER.error("Could not persist administrative user status audit", auditException);
        }
    }

    private void acquireLifecycleLock() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            String databaseProduct = connection.getMetaData().getDatabaseProductName()
                    .toLowerCase(Locale.ROOT);
            if (databaseProduct.contains("postgresql")) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT pg_advisory_xact_lock(hashtext(?))")) {
                    statement.setString(1, LIFECYCLE_LOCK_KEY);
                    statement.execute();
                }
            } else if (databaseProduct.contains("h2")) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM databasechangeloglock WHERE id = 1 FOR UPDATE")) {
                    statement.executeQuery();
                }
            } else {
                throw new IllegalStateException(
                        "Admin lifecycle requires PostgreSQL or H2 with a transactional lock.");
            }
            return null;
        });
    }
}
