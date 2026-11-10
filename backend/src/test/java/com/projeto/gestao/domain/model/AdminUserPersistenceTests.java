package com.projeto.gestao.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.dto.AdminUserResponse;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.service.AdminAuditService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserPersistenceTests {
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminAuditEventRepository auditEventRepository;
    @Autowired private AdminAuditService auditService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void persistsNormalizedAdministratorWithSafePasswordAndFixedRole() {
        AdminUser admin = AdminUser.create(
                UUID.randomUUID(), " Admin@Example.COM ", passwordEncoder.encode("SenhaAdmin1!"),
                OffsetDateTime.parse("2026-11-09T10:00:00-03:00"));

        adminUserRepository.saveAndFlush(admin);

        AdminUser persisted = adminUserRepository.findById(admin.getId()).orElseThrow();
        assertThat(persisted.getEmail()).isEqualTo("admin@example.com");
        assertThat(persisted.getStatus()).isEqualTo(AdminStatus.ACTIVE);
        assertThat(persisted.getRole()).isEqualTo(AdminRole.ADMIN);
        assertThat(persisted.passwordHash()).doesNotContain("SenhaAdmin1!").startsWith("$2");
        assertThat(passwordEncoder.matches("SenhaAdmin1!", persisted.passwordHash())).isTrue();
    }

    @Test
    void databaseRejectsCaseInsensitiveDuplicateNormalizedEmail() {
        adminUserRepository.saveAndFlush(AdminUser.create(
                UUID.randomUUID(), "admin@example.com", passwordEncoder.encode("SenhaAdmin1!"),
                OffsetDateTime.now()));

        AdminUser duplicate = AdminUser.create(
                UUID.randomUUID(), " ADMIN@EXAMPLE.COM ", passwordEncoder.encode("OutraSenha1!"),
                OffsetDateTime.now());

        assertThatThrownBy(() -> adminUserRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNonBcryptPasswordHashBeforePersistence() {
        assertThatThrownBy(() -> AdminUser.create(
                UUID.randomUUID(), "admin@example.com", "not-a-bcrypt-hash", OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash must be a BCrypt hash");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "$2a$03$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "$2a$32$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "$2a$99$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "$2x$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!",
        "$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "$2a$10aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    void rejectsStructurallyInvalidBcryptPasswordHashes(String passwordHash) {
        assertThatThrownBy(() -> AdminUser.create(
                UUID.randomUUID(), "admin@example.com", passwordHash, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash must be a BCrypt hash");
    }

    @Test
    void acceptsBcryptHashGeneratedByPasswordEncoder() {
        String passwordHash = passwordEncoder.encode("SenhaAdmin1!");

        AdminUser admin = AdminUser.create(
                UUID.randomUUID(), "admin@example.com", passwordHash, OffsetDateTime.now());

        assertThat(admin.passwordHash()).isEqualTo(passwordHash);
    }

    @Test
    void coversAdministratorLifecycleTransitionsAndGuards() {
        AdminUser admin = AdminUser.create(
                UUID.randomUUID(), "admin@example.com", passwordEncoder.encode("SenhaAdmin1!"),
                OffsetDateTime.now());

        admin.inactivate(OffsetDateTime.now());
        assertThat(admin.getStatus()).isEqualTo(AdminStatus.INACTIVE);
        assertThatThrownBy(() -> admin.inactivate(OffsetDateTime.now()))
                .isInstanceOf(IllegalStateException.class);

        admin.reactivate();
        assertThat(admin.getStatus()).isEqualTo(AdminStatus.ACTIVE);
        assertThatThrownBy(admin::reactivate)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingEmailAndPassword() {
        assertThatThrownBy(() -> AdminUser.create(
                UUID.randomUUID(), null, passwordEncoder.encode("SenhaAdmin1!"), OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdminUser.create(
                UUID.randomUUID(), " ", passwordEncoder.encode("SenhaAdmin1!"), OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdminUser.create(
                UUID.randomUUID(), "admin@example.com", null, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdminUser.create(
                UUID.randomUUID(), "admin@example.com", " ", OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void responseDtoAndAuditStorageExposeNoCredentialsOrFinancialFields() throws Exception {
        AdminUser admin = AdminUser.create(
                UUID.randomUUID(), "admin@example.com", passwordEncoder.encode("SenhaAdmin1!"),
                OffsetDateTime.now());
        adminUserRepository.saveAndFlush(admin);

        String response = objectMapper.writeValueAsString(AdminUserResponse.from(admin));
        assertThat(response).contains("admin@example.com", "ACTIVE", "ADMIN");
        assertThat(response).doesNotContain("password", "hash", "SenhaAdmin1!", "balance", "wallet", "position");

        auditService.record(null, AuditActorRole.ANONYMOUS, AdminAuditAction.ACCESS_DENIED,
                null, null, AdminAuditOutcome.DENIED, UUID.randomUUID());
        AdminAuditEvent event = auditEventRepository.findAll().get(0);
        assertThat(event.getActorRole()).isEqualTo(AuditActorRole.ANONYMOUS);
        assertThat(event.getAction()).isEqualTo(AdminAuditAction.ACCESS_DENIED);
        assertThat(event.getOutcome()).isEqualTo(AdminAuditOutcome.DENIED);
        assertThat(event.getActorAdminId()).isNull();
    }

    @Test
    void adminUserHasNoFinancialStateOrAccountRelationship() {
        assertThat(java.util.Arrays.stream(AdminUser.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("balance", "wallet", "position", "broker", "account", "cpf");
    }
}
