package com.projeto.gestao.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlAdminAuditMigrationRollbackTests {
    private static final String VALID_BCRYPT_HASH = "$2a$10$" + "a".repeat(53);
    private static final List<String> V14_ACTIONS = List.of(
            "BOOTSTRAP", "LOGIN", "LOGOUT", "LIST_INVESTORS",
            "ACTIVATE_INVESTOR", "DEACTIVATE_INVESTOR", "ACTIVATE_ADMIN",
            "DEACTIVATE_ADMIN", "ACCESS_DENIED");
    private static final List<String> ALL_ACTIONS = List.of(
            "BOOTSTRAP", "LOGIN", "LOGOUT", "LIST_INVESTORS",
            "ACTIVATE_INVESTOR", "DEACTIVATE_INVESTOR", "ACTIVATE_ADMIN",
            "DEACTIVATE_ADMIN", "ACCESS_DENIED", "RESET_ADMIN_PASSWORD");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void preservesV14AuditsAcross015RollbackReapplyAndRejectsUnsafeRollback() throws Exception {
        UUID adminId = UUID.randomUUID();
        JdbcTemplate jdbc = jdbc();
        applyPendingChangesets(15);
        assertThat(changeSetCount(jdbc, "v14-admin-lifecycle-audit-actions")).isEqualTo(1);
        assertThat(changeSetCount(jdbc, "v15-admin-password-reset-audit-action")).isZero();
        insertAdmin(jdbc, adminId);

        V14_ACTIONS.forEach(action -> insertAudit(jdbc, adminId, action));

        applyPendingChangesets(1);
        assertThat(changeSetCount(jdbc, "v15-admin-password-reset-audit-action")).isEqualTo(1);

        rollbackLatestChangeSet();
        assertThat(changeSetCount(jdbc, "v15-admin-password-reset-audit-action")).isZero();
        assertThat(auditActions(jdbc)).containsExactlyInAnyOrderElementsOf(V14_ACTIONS);
        assertThatThrownBy(() -> insertAudit(jdbc, adminId, "RESET_ADMIN_PASSWORD"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(auditActions(jdbc)).containsExactlyInAnyOrderElementsOf(V14_ACTIONS);

        applyPendingChangesets(1);
        insertAudit(jdbc, adminId, "RESET_ADMIN_PASSWORD");
        assertThat(auditActions(jdbc)).containsExactlyInAnyOrderElementsOf(ALL_ACTIONS);

        List<String> actionsBeforeUnsafeRollback = auditActions(jdbc);
        assertThatThrownBy(this::rollbackLatestChangeSet)
                .isInstanceOf(LiquibaseException.class);

        assertThat(changeSetCount(jdbc, "v15-admin-password-reset-audit-action")).isEqualTo(1);
        assertThat(auditActions(jdbc)).containsExactlyInAnyOrderElementsOf(
                actionsBeforeUnsafeRollback);
        assertThatCode(() -> insertAudit(jdbc, adminId, "RESET_ADMIN_PASSWORD"))
                .doesNotThrowAnyException();
    }

    private void applyPendingChangesets(int count) throws Exception {
        try (Connection connection = connection()) {
            Liquibase liquibase = liquibase(connection);
            liquibase.update(count, new Contexts(), new LabelExpression());
        }
    }

    private void rollbackLatestChangeSet() throws Exception {
        try (Connection connection = connection()) {
            Liquibase liquibase = liquibase(connection);
            liquibase.rollback(1, new Contexts(), new LabelExpression());
        }
    }

    private Liquibase liquibase(Connection connection) throws Exception {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        return new Liquibase(
                "db/changelog/db.changelog-master.xml",
                new ClassLoaderResourceAccessor(), database);
    }

    private Connection connection() throws Exception {
        return new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .getConnection();
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    private void insertAdmin(JdbcTemplate jdbc, UUID adminId) {
        jdbc.update("""
                INSERT INTO admin_user (id, email, password_hash, status, role, created_at)
                VALUES (?, ?, ?, 'ACTIVE', 'ADMIN', ?)
                """, adminId, "migration-rollback-" + adminId + "@example.com",
                VALID_BCRYPT_HASH, Timestamp.from(Instant.now()));
    }

    private void insertAudit(JdbcTemplate jdbc, UUID adminId, String action) {
        jdbc.update("""
                INSERT INTO admin_audit_event
                    (id, actor_admin_id, actor_role, action, target_admin_id,
                     occurred_at, outcome, correlation_id)
                VALUES (?, NULL, 'SYSTEM', ?, ?, ?, 'SUCCESS', ?)
                """, UUID.randomUUID(), action, adminId,
                Timestamp.from(Instant.now()), UUID.randomUUID());
    }

    private int changeSetCount(JdbcTemplate jdbc, String id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = ?", Integer.class, id);
    }

    private List<String> auditActions(JdbcTemplate jdbc) {
        return jdbc.queryForList(
                "SELECT action FROM admin_audit_event ORDER BY action", String.class);
    }
}
