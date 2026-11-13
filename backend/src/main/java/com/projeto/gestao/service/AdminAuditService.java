package com.projeto.gestao.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditEvent;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.security.AdminPrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAuditService.class);
    private final AdminAuditEventRepository repository;
    private final Clock clock;

    public AdminAuditService(AdminAuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public AdminAuditEvent record(
            UUID actorAdminId, AuditActorRole actorRole, AdminAuditAction action,
            UUID targetAccountId, UUID targetAdminId, AdminAuditOutcome outcome,
            UUID correlationId) {
        AdminAuditEvent event = AdminAuditEvent.record(
                UUID.randomUUID(), actorAdminId, actorRole, action, targetAccountId,
                targetAdminId, OffsetDateTime.now(clock), outcome,
                correlationId == null ? UUID.randomUUID() : correlationId);
        return repository.save(event);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public AdminAuditEvent recordInNewTransaction(
            UUID actorAdminId, AuditActorRole actorRole, AdminAuditAction action,
            UUID targetAccountId, UUID targetAdminId, AdminAuditOutcome outcome,
            UUID correlationId) {
        return recordEvent(actorAdminId, actorRole, action, targetAccountId,
                targetAdminId, outcome, correlationId);
    }

    public void recordAccessDenied(Authentication authentication, String correlationHeader) {
        UUID actorAdminId = authentication != null
                && authentication.getPrincipal() instanceof AdminPrincipal principal
                ? principal.adminId() : null;
        AuditActorRole actorRole = actorAdminId == null
                ? AuditActorRole.ANONYMOUS : AuditActorRole.ADMIN;
        try {
            recordInNewTransaction(actorAdminId, actorRole, AdminAuditAction.ACCESS_DENIED,
                    null, null, AdminAuditOutcome.DENIED, correlationId(correlationHeader));
        } catch (RuntimeException exception) {
            LOGGER.error("Could not persist administrative access denial audit", exception);
        }
    }

    public static UUID correlationId(String correlationHeader) {
        if (correlationHeader != null && !correlationHeader.isBlank()) {
            try {
                return UUID.fromString(correlationHeader);
            } catch (IllegalArgumentException ignored) {
                // Generate a safe internal correlation when the header is malformed.
            }
        }
        return UUID.randomUUID();
    }

    private AdminAuditEvent recordEvent(
            UUID actorAdminId, AuditActorRole actorRole, AdminAuditAction action,
            UUID targetAccountId, UUID targetAdminId, AdminAuditOutcome outcome,
            UUID correlationId) {
        AdminAuditEvent event = AdminAuditEvent.record(
                UUID.randomUUID(), actorAdminId, actorRole, action, targetAccountId,
                targetAdminId, OffsetDateTime.now(clock), outcome,
                correlationId == null ? UUID.randomUUID() : correlationId);
        return repository.save(event);
    }
}
