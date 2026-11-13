package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminUserRepository;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AdminBootstrapServiceTests {
    private static final long TIMEOUT_SECONDS = 10;

    @Autowired private AdminBootstrapService bootstrapService;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminAuditEventRepository auditEventRepository;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        adminUserRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
        auditEventRepository.deleteAll();
        adminUserRepository.deleteAll();
    }

    @Test
    void createsFirstAdministratorAndAuditsTheBootstrap() {
        AdminBootstrapService.BootstrapResult result =
                bootstrapService.bootstrap(" ADMIN@Example.COM ", "SenhaAdmin1!");

        assertThat(result.created()).isTrue();
        assertThat(adminUserRepository.count()).isEqualTo(1);
        assertThat(auditEventRepository.count()).isEqualTo(1);
        assertThat(auditEventRepository.findAll().get(0).getAction())
                .isEqualTo(com.projeto.gestao.domain.model.AdminAuditAction.BOOTSTRAP);
    }

    @Test
    void repeatedBootstrapIsIdempotentAndDoesNotReplaceCredentials() {
        AdminBootstrapService.BootstrapResult first =
                bootstrapService.bootstrap("first@example.com", "SenhaAdmin1!");
        AdminBootstrapService.BootstrapResult second =
                bootstrapService.bootstrap("second@example.com", "OutraSenha1!");

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(adminUserRepository.count()).isEqualTo(1);
        assertThat(adminUserRepository.findAll().get(0).getEmail()).isEqualTo("first@example.com");
        assertThat(auditEventRepository.count()).isEqualTo(2);
    }

    @Test
    void missingCredentialsFailsBeforeCreatingAdministrator() {
        assertThatThrownBy(() -> bootstrapService.bootstrap("admin@example.com", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A senha de bootstrap é obrigatória.");
        assertThat(adminUserRepository.count()).isZero();
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void nullCredentialsFailBeforeCreatingAdministrator() {
        assertThatThrownBy(() -> bootstrapService.bootstrap("admin@example.com", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(adminUserRepository.count()).isZero();
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void postgresDatabaseUsesTransactionalAdvisoryLock() throws Exception {
        AdminUserRepository users = mock(AdminUserRepository.class);
        AdminAuditService audit = mock(AdminAuditService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(users.count()).thenReturn(0L);
        when(encoder.encode("SenhaAdmin1!")).thenReturn(
                "$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        executeCallbackWith(connection, jdbc);

        AdminBootstrapService service = new AdminBootstrapService(
                users, audit, encoder, jdbc, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        AdminBootstrapService.BootstrapResult result =
                service.bootstrap("admin@example.com", "SenhaAdmin1!");

        assertThat(result.created()).isTrue();
        verify(statement).setString(1, "corretora.admin.bootstrap");
        verify(statement).execute();
    }

    @Test
    void unsupportedDatabaseIsRejectedBeforeCreatingAdministrator() throws Exception {
        AdminUserRepository users = mock(AdminUserRepository.class);
        AdminAuditService audit = mock(AdminAuditService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("SQLite");
        executeCallbackWith(connection, jdbc);

        AdminBootstrapService service = new AdminBootstrapService(
                users, audit, encoder, jdbc, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.bootstrap("admin@example.com", "SenhaAdmin1!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Bootstrap administrativo exige PostgreSQL ou H2 de teste com lock transacional.");
        verifyNoInteractions(users, audit, encoder);
    }

    @SuppressWarnings("unchecked")
    private static void executeCallbackWith(Connection connection, JdbcTemplate jdbc) {
        doAnswer(invocation -> {
            ConnectionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        }).when(jdbc).execute(any(ConnectionCallback.class));
    }

    @Test
    void concurrentBootstrapsCreateOnlyOneAdministrator() throws Exception {
        executor = Executors.newFixedThreadPool(4);
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch release = new CountDownLatch(1);
        List<Future<AdminBootstrapService.BootstrapResult>> futures = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                if (!release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new AssertionError("barreira nao foi liberada");
                }
                return bootstrapService.bootstrap("admin" + index + "@example.com", "SenhaAdmin1!");
            }));
        }

        assertThat(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        List<AdminBootstrapService.BootstrapResult> results = new ArrayList<>();
        for (Future<AdminBootstrapService.BootstrapResult> future : futures) {
            results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        assertThat(results.stream().filter(AdminBootstrapService.BootstrapResult::created).count())
                .isEqualTo(1);
        assertThat(adminUserRepository.count()).isEqualTo(1);
    }
}
