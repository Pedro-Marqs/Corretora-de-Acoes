package com.projeto.gestao.domain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminRole;
import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.domain.model.AdminUser;

public record AdminUserResponse(
        UUID id, String email, AdminStatus status, AdminRole role,
        OffsetDateTime createdAt, OffsetDateTime inactivatedAt) {

    public static AdminUserResponse from(AdminUser adminUser) {
        return new AdminUserResponse(
                adminUser.getId(), adminUser.getEmail(), adminUser.getStatus(),
                adminUser.getRole(), adminUser.getCreatedAt(), adminUser.getInactivatedAt());
    }
}
