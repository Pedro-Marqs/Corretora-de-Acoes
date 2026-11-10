package com.projeto.gestao.service;

import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AdminRole;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.repository.AdminUserRepository;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {
    private static final String ADVISORY_LOCK_KEY = "corretora.admin.bootstrap";

    private final AdminUserRepository adminUserRepository;
    private final AdminAuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public AdminBootstrapService(
            AdminUserRepository adminUserRepository, AdminAuditService auditService,
            PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate, Clock clock) {
        this.adminUserRepository = adminUserRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Transactional
    public BootstrapResult bootstrap(String email, String rawPassword) {
        String normalizedEmail = AdminUser.normalizeEmail(email);
        requirePassword(rawPassword);

        acquireDatabaseLock();
        if (adminUserRepository.count() > 0) {
            AdminUser existing = adminUserRepository.findFirstByOrderByCreatedAtAsc().orElseThrow();
            auditService.record(null, AuditActorRole.SYSTEM, AdminAuditAction.BOOTSTRAP,
                    null, existing.getId(), AdminAuditOutcome.SUCCESS, null);
            return new BootstrapResult(false, existing.getId());
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        AdminUser adminUser = AdminUser.create(
                UUID.randomUUID(), normalizedEmail, passwordEncoder.encode(rawPassword), now);
        adminUserRepository.saveAndFlush(adminUser);
        auditService.record(null, AuditActorRole.SYSTEM, AdminAuditAction.BOOTSTRAP,
                null, adminUser.getId(), AdminAuditOutcome.SUCCESS, null);
        return new BootstrapResult(true, adminUser.getId());
    }

    private void acquireDatabaseLock() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            String databaseProduct = connection.getMetaData().getDatabaseProductName()
                    .toLowerCase(Locale.ROOT);
            if (databaseProduct.contains("postgresql")) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT pg_advisory_xact_lock(hashtext(?))")) {
                    statement.setString(1, ADVISORY_LOCK_KEY);
                    statement.execute();
                }
            } else if (databaseProduct.contains("h2")) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM databasechangeloglock WHERE id = 1 FOR UPDATE")) {
                    statement.executeQuery();
                }
            } else {
                throw new IllegalStateException(
                        "Bootstrap administrativo exige PostgreSQL ou H2 de teste com lock transacional.");
            }
            return null;
        });
    }

    private void requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("A senha de bootstrap é obrigatória.");
        }
    }

    public record BootstrapResult(boolean created, UUID adminId) {
        public AdminRole role() {
            return AdminRole.ADMIN;
        }
    }
}
