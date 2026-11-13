package com.projeto.gestao.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "ix_refresh_token_account", columnList = "account_id"),
        @Index(name = "ix_refresh_token_family", columnList = "family_id")
})
public class RefreshToken {
    @Id private UUID id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "account_id", nullable = false) private UUID accountId;
    @Column(name = "family_id", nullable = false) private UUID familyId;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "expires_at", nullable = false) private OffsetDateTime expiresAt;
    @Column(name = "revoked_at") private OffsetDateTime revokedAt;
    @Column(name = "replaced_by_id") private UUID replacedById;

    protected RefreshToken() { }

    public static RefreshToken issue(
            UUID id, String tokenHash, UUID accountId, UUID familyId,
            OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.id = id;
        token.tokenHash = tokenHash;
        token.accountId = accountId;
        token.familyId = familyId;
        token.createdAt = createdAt;
        token.expiresAt = expiresAt;
        return token;
    }

    public void rotateTo(UUID replacementId, OffsetDateTime occurredAt) {
        if (revokedAt != null || replacedById != null) {
            throw new IllegalStateException("Refresh token is not active");
        }
        revokedAt = occurredAt;
        replacedById = replacementId;
    }

    public void revoke(OffsetDateTime occurredAt) {
        if (revokedAt == null) {
            revokedAt = occurredAt;
        }
    }

    public boolean isExpired(OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isConsumed() {
        return replacedById != null;
    }

    public UUID getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public UUID getAccountId() { return accountId; }
    public UUID getFamilyId() { return familyId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public UUID getReplacedById() { return replacedById; }
}
