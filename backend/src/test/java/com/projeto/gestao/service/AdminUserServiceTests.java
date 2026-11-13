package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.repository.AdminUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTests {
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-11T12:00:00Z");
    private static final String VALID_BCRYPT_HASH = "$2a$10$" + "a".repeat(53);

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private ObjectProvider<AdminRefreshTokenService> refreshTokenServiceProvider;
    @Mock private AdminAuditService auditService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private Connection connection;
    @Mock private DatabaseMetaData databaseMetaData;
    @Mock private PreparedStatement preparedStatement;

    private AdminUserService service;
    private AdminUser actor;
    private AdminUser target;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(adminUserRepository, refreshTokenServiceProvider,
                auditService, jdbcTemplate, Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
        actor = admin(ACTOR_ID);
        target = admin(TARGET_ID);
    }

    @Test
    void postgresqlAdvisoryLockIsUsedWhenRefreshServiceIsUnavailable() throws Exception {
        target.inactivate(NOW.minusMinutes(1));
        stubDatabase("PostgreSQL");
        when(adminUserRepository.findByIdAndStatus(ACTOR_ID, AdminStatus.ACTIVE))
                .thenReturn(Optional.of(actor));
        when(adminUserRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        service.changeStatus(TARGET_ID, AdminStatus.ACTIVE, ACTOR_ID, CORRELATION_ID);

        verify(preparedStatement).setString(1, "corretora.admin.lifecycle");
        verify(preparedStatement).execute();
        verify(auditService).record(ACTOR_ID, AuditActorRole.ADMIN,
                AdminAuditAction.ACTIVATE_ADMIN, null, TARGET_ID,
                AdminAuditOutcome.SUCCESS, CORRELATION_ID);
    }

    @Test
    void invalidTransitionRemainsBusinessConflictWhenRejectedAuditFails() throws Exception {
        stubDatabase("H2");
        when(adminUserRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
        doThrow(new IllegalStateException("audit unavailable"))
                .when(auditService).recordInNewTransaction(
                        eq(ACTOR_ID), eq(AuditActorRole.ADMIN),
                        eq(AdminAuditAction.ACTIVATE_ADMIN), eq(null), eq(TARGET_ID),
                        eq(AdminAuditOutcome.DENIED), eq(CORRELATION_ID));

        assertThatThrownBy(() -> service.changeStatus(
                TARGET_ID, AdminStatus.ACTIVE, ACTOR_ID, CORRELATION_ID))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void nullTransitionIsRejectedAndAuditedAsDenied() throws Exception {
        stubDatabase("H2");
        when(adminUserRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.changeStatus(
                TARGET_ID, null, ACTOR_ID, CORRELATION_ID))
                .isInstanceOf(ConflictException.class);

        verify(auditService).recordInNewTransaction(ACTOR_ID, AuditActorRole.ADMIN,
                AdminAuditAction.DEACTIVATE_ADMIN, null, TARGET_ID,
                AdminAuditOutcome.DENIED, CORRELATION_ID);
    }

    private void stubDatabase(String productName) throws Exception {
        when(jdbcTemplate.execute(org.mockito.ArgumentMatchers.<ConnectionCallback<Void>>any()))
                .thenAnswer(invocation -> {
            ConnectionCallback<Void> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        });
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn(productName);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    private AdminUser admin(UUID id) {
        return AdminUser.create(id, id + "@example.com", VALID_BCRYPT_HASH, NOW);
    }
}
