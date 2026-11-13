package com.projeto.gestao.service;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.repository.AdminUserRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.security.admin-jwt", name = "enabled", havingValue = "true")
public class AdminPasswordResetService {
    private final AdminUserRepository adminUserRepository;
    private final AdminRefreshTokenService refreshTokenService;
    private final AdminAuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AdminPasswordResetService(
            AdminUserRepository adminUserRepository,
            AdminRefreshTokenService refreshTokenService,
            AdminAuditService auditService,
            PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UUID reset(String email, char[] rawPassword) {
        try {
            String normalizedEmail = AdminUser.normalizeEmail(email);
            requirePassword(rawPassword);
            AdminUser admin = adminUserRepository.findForUpdateByEmailIgnoreCase(normalizedEmail)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Administrador existente não encontrado."));

            /*
             * PasswordEncoder accepts CharSequence and may create an internal String.
             * The mutable input is cleared below, but Java cannot guarantee erasure
             * of encoder-owned immutable copies.
             */
            admin.changePasswordHash(passwordEncoder.encode(CharBuffer.wrap(rawPassword)));
            adminUserRepository.saveAndFlush(admin);

            refreshTokenService.revokeAllForAdmin(admin.getId());
            auditService.record(null, AuditActorRole.SYSTEM,
                    AdminAuditAction.RESET_ADMIN_PASSWORD, null, admin.getId(),
                    AdminAuditOutcome.SUCCESS, null);
            return admin.getId();
        } finally {
            clear(rawPassword);
        }
    }

    private void requirePassword(char[] rawPassword) {
        if (isBlank(rawPassword)) {
            throw new IllegalArgumentException(
                    "A nova senha administrativa é obrigatória.");
        }
    }

    private static boolean isBlank(char[] value) {
        if (value == null || value.length == 0) {
            return true;
        }
        for (char character : value) {
            if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return true;
    }

    private static void clear(char[] rawPassword) {
        if (rawPassword != null) {
            Arrays.fill(rawPassword, '\0');
        }
    }
}
