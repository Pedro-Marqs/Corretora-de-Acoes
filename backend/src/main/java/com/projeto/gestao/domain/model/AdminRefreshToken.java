package com.projeto.gestao.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_refresh_token", indexes = {
        @Index(name = "ix_admin_refresh_token_admin", columnList = "admin_user_id"),
        @Index(name = "ix_admin_refresh_token_family", columnList = "family_id")
})
public class AdminRefreshToken {
    @Id private UUID id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "admin_user_id", nullable = false) private UUID adminUserId;
    @Column(name = "family_id", nullable = false) private UUID familyId;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "expires_at", nullable = false) private OffsetDateTime expiresAt;
    @Column(name = "revoked_at") private OffsetDateTime revokedAt;
    @Column(name = "replaced_by_id") private UUID replacedById;

    protected AdminRefreshToken() { }

    public static AdminRefreshToken issue(
            UUID id, String tokenHash, UUID adminUserId, UUID familyId,
            OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        AdminRefreshToken token = new AdminRefreshToken();
        token.id = id;
        token.tokenHash = tokenHash;
        token.adminUserId = adminUserId;
        token.familyId = familyId;
        token.createdAt = createdAt;
        token.expiresAt = expiresAt;
        return token;
    }

    public void rotateTo(UUID replacementId, OffsetDateTime occurredAt) {
        if (revokedAt != null || replacedById != null) {
            throw new IllegalStateException("Admin refresh token is not active");
        }
        revokedAt = occurredAt;
        replacedById = replacementId;
    }

    public boolean isExpired(OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isConsumed() {
        return replacedById != null;
    }

    public UUID getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public UUID getAdminUserId() { return adminUserId; }
    public UUID getFamilyId() { return familyId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public UUID getReplacedById() { return replacedById; }
}
