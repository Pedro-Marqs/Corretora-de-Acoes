package com.projeto.gestao.api.controller;

import java.util.UUID;

import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.security.AdminPrincipal;
import com.projeto.gestao.service.AdminAuditService;
import com.projeto.gestao.service.AdminUserService;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/admins")
@ConditionalOnProperty(prefix = "app.security.admin-jwt", name = "enabled", havingValue = "true")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PatchMapping("/{adminId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') and principal instanceof T(com.projeto.gestao.security.AdminPrincipal)")
    void changeStatus(
            @PathVariable UUID adminId,
            @Valid @RequestBody AdminUserStatusRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationHeader) {
        adminUserService.changeStatus(adminId, AdminStatus.valueOf(request.status().name()),
                principal.adminId(), AdminAuditService.correlationId(correlationHeader));
    }
}
