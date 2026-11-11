package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditEvent;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminRefreshTokenRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.security.AdminJwtTokenService;
import com.projeto.gestao.security.AdminPrincipal;
import com.projeto.gestao.security.JwtTokenService;
import com.projeto.gestao.service.AdminRefreshTokenService;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserManagementTests {
    private static final String ADMIN_SECRET = testSecret();
    private static final String INVESTOR_SECRET = testSecret();
    private static final String PASSWORD = "Senha Admin Lifecycle 1!";

    @Autowired private MockMvc mockMvc;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminRefreshTokenRepository refreshTokenRepository;
    @Autowired private AdminAuditEventRepository auditRepository;
    @Autowired private AdminJwtTokenService adminJwtTokenService;
    @Autowired private JwtTokenService investorJwtTokenService;
    @Autowired private AdminRefreshTokenService refreshTokenService;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private UUID actorId;
    private UUID targetId;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "false");
        registry.add("app.security.jwt.issuer", () -> "corretora-investor-lifecycle");
        registry.add("app.security.jwt.audience", () -> "corretora-investors-lifecycle");
        registry.add("app.security.jwt.secret", () -> INVESTOR_SECRET);
        registry.add("app.security.admin-jwt.enabled", () -> "true");
        registry.add("app.security.admin-jwt.issuer", () -> "corretora-admin-lifecycle");
        registry.add("app.security.admin-jwt.audience", () -> "corretora-admin-api-lifecycle");
        registry.add("app.security.admin-jwt.secret", () -> ADMIN_SECRET);
    }

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        auditRepository.deleteAll();
        adminUserRepository.deleteAll();
        actorId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        adminUserRepository.saveAndFlush(admin(actorId));
        adminUserRepository.saveAndFlush(admin(targetId));
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        auditRepository.deleteAll();
        adminUserRepository.deleteAll();
    }

    @Test
    void validDeactivationRevokesAllTargetFamiliesAndExistingAccessToken() throws Exception {
        AdminRefreshTokenService.IssuedRefreshToken first =
                refreshTokenService.issue(new AdminPrincipal(targetId));
        AdminRefreshTokenService.IssuedRefreshToken second =
                refreshTokenService.issue(new AdminPrincipal(targetId));
        refreshTokenService.issue(new AdminPrincipal(actorId));
        String targetAccessToken = adminJwtTokenService.issue(new AdminPrincipal(targetId)).tokenValue();

        deactivate(actorId, targetId).andExpect(status().isNoContent());

        assertThat(adminUserRepository.findById(targetId).orElseThrow().getStatus())
                .isEqualTo(AdminStatus.INACTIVE);
        assertThat(refreshTokenRepository.findAll()).filteredOn(
                token -> token.getAdminUserId().equals(targetId))
                .allMatch(token -> token.getRevokedAt() != null);
        assertThat(refreshTokenRepository.findAll()).filteredOn(
                token -> token.getAdminUserId().equals(actorId))
                .allMatch(token -> token.getRevokedAt() == null);

        mockMvc.perform(get("/api/admin/dashboard").param("period", "4W")
                        .header("Authorization", "Bearer " + targetAccessToken))
                .andExpect(status().isUnauthorized());
        refresh(first.tokenValue()).andExpect(status().isUnauthorized());
        refresh(second.tokenValue()).andExpect(status().isUnauthorized());
        login(targetId).andExpect(status().isUnauthorized());

        AdminAuditEvent audit = lastAudit(AdminAuditAction.DEACTIVATE_ADMIN);
        assertThat(audit.getActorAdminId()).isEqualTo(actorId);
        assertThat(audit.getTargetAdminId()).isEqualTo(targetId);
        assertThat(audit.getOutcome()).isEqualTo(AdminAuditOutcome.SUCCESS);
        assertThat(audit.getCorrelationId()).isNotNull();
    }

    @Test
    void lastActiveAdministratorReturnsConflictWithoutChangingStateOrRevokingSessions() throws Exception {
        AdminUser target = adminUserRepository.findById(targetId).orElseThrow();
        target.inactivate(OffsetDateTime.now());
        adminUserRepository.saveAndFlush(target);
        AdminUser onlyAdmin = adminUserRepository.findById(actorId).orElseThrow();
        refreshTokenService.issue(new AdminPrincipal(actorId));

        deactivate(actorId, actorId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT_ERROR"));

        assertThat(adminUserRepository.findById(actorId).orElseThrow().getStatus())
                .isEqualTo(AdminStatus.ACTIVE);
        assertThat(refreshTokenRepository.findAll()).filteredOn(
                token -> token.getAdminUserId().equals(actorId))
                .allMatch(token -> token.getRevokedAt() == null);

        AdminAuditEvent audit = lastAudit(AdminAuditAction.DEACTIVATE_ADMIN);
        assertThat(audit.getActorAdminId()).isEqualTo(actorId);
        assertThat(audit.getTargetAdminId()).isEqualTo(actorId);
        assertThat(audit.getOutcome()).isEqualTo(AdminAuditOutcome.DENIED);
        assertThat(onlyAdmin.getStatus()).isEqualTo(AdminStatus.ACTIVE);
    }

    @Test
    void missingAndRepeatedTransitionsReturnContractErrors() throws Exception {
        UUID missing = UUID.randomUUID();
        deactivate(actorId, missing).andExpect(status().isNotFound());
        deactivate(actorId, targetId).andExpect(status().isNoContent());
        deactivate(actorId, targetId).andExpect(status().isConflict());
        assertThat(adminUserRepository.findById(targetId).orElseThrow().getStatus())
                .isEqualTo(AdminStatus.INACTIVE);
        assertThat(audits(AdminAuditAction.DEACTIVATE_ADMIN)).extracting(AdminAuditEvent::getOutcome)
                .contains(AdminAuditOutcome.SUCCESS, AdminAuditOutcome.DENIED);
    }

    @Test
    void activeTransitionReactivatesAdministratorAndAuditsSuccess() throws Exception {
        deactivate(actorId, targetId).andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/admin/admins/{id}/status", targetId)
                        .header("Authorization", "Bearer " + adminToken(actorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNoContent());

        assertThat(adminUserRepository.findById(targetId).orElseThrow().getStatus())
                .isEqualTo(AdminStatus.ACTIVE);
        assertThat(lastAudit(AdminAuditAction.ACTIVATE_ADMIN).getOutcome())
                .isEqualTo(AdminAuditOutcome.SUCCESS);
    }

    @Test
    void authenticationAndAuthorizationAreRequiredForAdministratorLifecycle() throws Exception {
        mockMvc.perform(patch("/api/admin/admins/{id}/status", targetId)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isUnauthorized());

        String investorToken = investorJwtTokenService.issue(
                new com.projeto.gestao.security.AccountPrincipal(UUID.randomUUID())).tokenValue();
        mockMvc.perform(patch("/api/admin/admins/{id}/status", targetId)
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isUnauthorized());

        AdminUser inactiveActor = adminUserRepository.findById(actorId).orElseThrow();
        inactiveActor.inactivate(OffsetDateTime.now());
        adminUserRepository.saveAndFlush(inactiveActor);
        String inactiveToken = adminJwtTokenService.issue(new AdminPrincipal(actorId)).tokenValue();
        mockMvc.perform(patch("/api/admin/admins/{id}/status", targetId)
                        .header("Authorization", "Bearer " + inactiveToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidStatusBodyReturnsBadRequestWithoutExposingCredentials() throws Exception {
        String response = mockMvc.perform(patch("/api/admin/admins/{id}/status", targetId)
                        .header("Authorization", "Bearer " + adminToken(actorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BROKEN\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("secret", "passwordHash", "token");
    }

    private org.springframework.test.web.servlet.ResultActions deactivate(
            UUID actor, UUID target) throws Exception {
        return mockMvc.perform(patch("/api/admin/admins/{id}/status", target)
                .header("Authorization", "Bearer " + adminToken(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/admin/auth/refresh").with(csrf())
                .cookie(new Cookie(AdminAuthenticationController.REFRESH_COOKIE, token)));
    }

    private org.springframework.test.web.servlet.ResultActions login(UUID id) throws Exception {
        return mockMvc.perform(post("/api/admin/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + id + "@example.com\",\"password\":\""
                        + PASSWORD + "\"}"));
    }

    private String adminToken(UUID id) {
        return adminJwtTokenService.issue(new AdminPrincipal(id)).tokenValue();
    }

    private AdminUser admin(UUID id) {
        return AdminUser.create(id, id + "@example.com", passwordEncoder.encode(PASSWORD),
                OffsetDateTime.now());
    }

    private AdminAuditEvent lastAudit(AdminAuditAction action) {
        return audits(action).get(audits(action).size() - 1);
    }

    private List<AdminAuditEvent> audits(AdminAuditAction action) {
        return auditRepository.findAll().stream().filter(event -> event.getAction() == action).toList();
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
