package com.projeto.gestao.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "account_broker", uniqueConstraints = @UniqueConstraint(
        name = "uq_account_broker_account_broker", columnNames = {"account_id", "broker_id"}))
public class AccountBroker {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id") private Account account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "broker_id") private Broker broker;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable = false, length = 16) private AssociationStatus status;
    @Column(name = "associated_at", nullable = false) private OffsetDateTime associatedAt;
    @Column(name = "removed_at") private OffsetDateTime removedAt;

    protected AccountBroker() { }

    public static AccountBroker create(UUID id, Account account, Broker broker, OffsetDateTime associatedAt) {
        if (id == null || account == null || broker == null || associatedAt == null) {
            throw new IllegalArgumentException("Association data is required");
        }
        AccountBroker association = new AccountBroker();
        association.id = id;
        association.account = account;
        association.broker = broker;
        association.status = AssociationStatus.ACTIVE;
        association.associatedAt = associatedAt;
        return association;
    }

    public void inactivate(OffsetDateTime occurredAt) {
        if (status != AssociationStatus.ACTIVE || occurredAt == null) {
            throw new IllegalStateException("Only an active association can be inactivated");
        }
        status = AssociationStatus.INACTIVE;
        removedAt = occurredAt;
    }

    public void reactivate() {
        if (status != AssociationStatus.INACTIVE) {
            throw new IllegalStateException("Only an inactive association can be reactivated");
        }
        status = AssociationStatus.ACTIVE;
        removedAt = null;
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public Broker getBroker() { return broker; }
    public AssociationStatus getStatus() { return status; }
    public OffsetDateTime getAssociatedAt() { return associatedAt; }
    public OffsetDateTime getRemovedAt() { return removedAt; }
}
