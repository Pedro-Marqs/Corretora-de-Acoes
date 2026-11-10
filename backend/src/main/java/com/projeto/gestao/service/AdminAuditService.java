package com.projeto.gestao.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditEvent;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.repository.AdminAuditEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {
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
}
