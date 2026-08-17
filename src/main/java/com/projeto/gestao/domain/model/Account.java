package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
@Table(name = "account")
public class Account {
    @Id private UUID id;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, length = 11) private String cpf;
    @Column(nullable = false, length = 254) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal balance;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable = false, length = 16) private AccountStatus status;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "inactivated_at") private OffsetDateTime inactivatedAt;

    protected Account() { }

    public static Account create(
            UUID id, String name, String cpf, String email, String passwordHash,
            BigDecimal balance, OffsetDateTime createdAt) {
        Account account = new Account();
        account.id = id;
        account.name = name;
        account.cpf = cpf;
        account.email = email;
        account.passwordHash = passwordHash;
        account.balance = balance;
        account.status = AccountStatus.ACTIVE;
        account.createdAt = createdAt;
        return account;
    }

    public void changeEmail(String newEmail) {
        this.email = newEmail;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void inactivate(OffsetDateTime occurredAt) {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Only an active account can be inactivated");
        }
        status = AccountStatus.INACTIVE;
        inactivatedAt = occurredAt;
    }

    public void reactivate() {
        if (status != AccountStatus.INACTIVE) {
            throw new IllegalStateException("Only an inactive account can be reactivated");
        }
        status = AccountStatus.ACTIVE;
        inactivatedAt = null;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getInactivatedAt() { return inactivatedAt; }
}
