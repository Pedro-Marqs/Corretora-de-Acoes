package com.projeto.gestao.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "admin_audit_event")
public class AdminAuditEvent {
    @Id private UUID id;
    @Column(name = "actor_admin_id") private UUID actorAdminId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "actor_role", nullable = false, length = 16) private AuditActorRole actorRole;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 64) private AdminAuditAction action;
    @Column(name = "target_account_id") private UUID targetAccountId;
    @Column(name = "target_admin_id") private UUID targetAdminId;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16) private AdminAuditOutcome outcome;
    @Column(name = "correlation_id", nullable = false) private UUID correlationId;

    protected AdminAuditEvent() { }

    public static AdminAuditEvent record(
            UUID id, UUID actorAdminId, AuditActorRole actorRole, AdminAuditAction action,
            UUID targetAccountId, UUID targetAdminId, OffsetDateTime occurredAt,
            AdminAuditOutcome outcome, UUID correlationId) {
        AdminAuditEvent event = new AdminAuditEvent();
        event.id = Objects.requireNonNull(id, "id is required");
        event.actorAdminId = actorAdminId;
        event.actorRole = Objects.requireNonNull(actorRole, "actorRole is required");
        event.action = Objects.requireNonNull(action, "action is required");
        event.targetAccountId = targetAccountId;
        event.targetAdminId = targetAdminId;
        event.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        event.outcome = Objects.requireNonNull(outcome, "outcome is required");
        event.correlationId = Objects.requireNonNull(correlationId, "correlationId is required");
        return event;
    }

    public UUID getId() { return id; }
    public UUID getActorAdminId() { return actorAdminId; }
    public AuditActorRole getActorRole() { return actorRole; }
    public AdminAuditAction getAction() { return action; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public UUID getTargetAdminId() { return targetAdminId; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public AdminAuditOutcome getOutcome() { return outcome; }
    public UUID getCorrelationId() { return correlationId; }
}
