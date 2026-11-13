package com.projeto.gestao.domain.model;

import java.time.OffsetDateTime;
import java.util.Locale;
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
@Table(name = "admin_user")
public class AdminUser {
    @Id private UUID id;
    @Column(nullable = false, length = 254) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16) private AdminStatus status;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16) private AdminRole role;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "inactivated_at") private OffsetDateTime inactivatedAt;

    protected AdminUser() { }

    public static AdminUser create(
            UUID id, String email, String passwordHash, OffsetDateTime createdAt) {
        AdminUser admin = new AdminUser();
        admin.id = Objects.requireNonNull(id, "id is required");
        admin.email = normalizeEmail(email);
        admin.passwordHash = requireBcryptHash(passwordHash);
        admin.status = AdminStatus.ACTIVE;
        admin.role = AdminRole.ADMIN;
        admin.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        return admin;
    }

    public static String normalizeEmail(String email) {
        return requireText(email, "email").trim().toLowerCase(Locale.ROOT);
    }

    public void inactivate(OffsetDateTime occurredAt) {
        if (status != AdminStatus.ACTIVE) {
            throw new IllegalStateException("Only an active administrator can be inactivated");
        }
        status = AdminStatus.INACTIVE;
        inactivatedAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    public void reactivate() {
        if (status != AdminStatus.INACTIVE) {
            throw new IllegalStateException("Only an inactive administrator can be reactivated");
        }
        status = AdminStatus.ACTIVE;
        inactivatedAt = null;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String passwordHash() { return passwordHash; }
    public AdminStatus getStatus() { return status; }
    public AdminRole getRole() { return role; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getInactivatedAt() { return inactivatedAt; }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = requireBcryptHash(passwordHash);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static String requireBcryptHash(String passwordHash) {
        String value = requireText(passwordHash, "passwordHash");
        if (value.length() != 60
                || !value.matches("\\A\\$2[aby]\\$(0[4-9]|[12]\\d|3[01])\\$[./A-Za-z0-9]{53}\\z")) {
            throw new IllegalArgumentException("passwordHash must be a BCrypt hash");
        }
        return value;
    }
}
