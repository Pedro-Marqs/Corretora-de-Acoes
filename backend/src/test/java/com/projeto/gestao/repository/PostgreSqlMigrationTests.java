package com.projeto.gestao.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationTests {
    private static final String VALID_BCRYPT_HASH = "$2a$10$" + "a".repeat(53);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrateSchema() throws Exception {
        try (Connection connection = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
        }
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    void migratesEmptyPostgreSqlDatabase() {
        Integer appliedMigrations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE exectype = 'EXECUTED'", Integer.class);
        assertThat(appliedMigrations).isEqualTo(14);
    }

    @Test
    void enforcesCriticalPostgreSqlConstraints() {
        insertAccount(UUID.randomUUID(), "55555555555", "postgres@example.com", "ACTIVE");

        assertThatThrownBy(() -> insertAccount(
                UUID.randomUUID(), "55555555555", "other-postgres@example.com", "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAccount(
                UUID.randomUUID(), "66666666666", "negative-postgres@example.com", "ACTIVE",
                new BigDecimal("-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesT14HistoricalSchema() {
        List<String> columns = jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                 WHERE table_name = 'patrimonial_point'
                """, String.class);
        assertThat(columns).contains("balance_brl", "positions_value_brl", "usd_brl_rate");

        UUID accountId = UUID.randomUUID();
        insertAccount(accountId, "90909090909", "t14-postgres@example.com", "ACTIVE");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO movement
                    (id, account_id, movement_type, ticker, total_amount, currency,
                     occurred_at, remaining_balance)
                VALUES (?, ?, 'DEPOSIT', 'PETR4', 10.00, 'BRL', ?, 10010.00)
                """, UUID.randomUUID(), accountId, Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void createsAdministrativeFoundationWithoutFinancialRelationsOnAdminUser() {
        List<String> tables = jdbc.queryForList("""
                SELECT LOWER(table_name) FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_name IN ('admin_user', 'admin_refresh_token', 'admin_audit_event')
                """, String.class);
        assertThat(tables).containsExactlyInAnyOrder(
                "admin_user", "admin_refresh_token", "admin_audit_event");

        List<String> adminColumns = jdbc.queryForList("""
                SELECT LOWER(column_name) FROM information_schema.columns
                 WHERE table_name = 'admin_user'
                """, String.class);
        assertThat(adminColumns).containsExactlyInAnyOrder(
                "id", "email", "password_hash", "status", "role", "created_at", "inactivated_at");
    }

    @Test
    void enforcesAdministrativeConstraintsAndIndexes() {
        UUID adminId = UUID.randomUUID();
        insertAdmin(adminId, "admin-constraints-" + adminId + "@example.com",
                VALID_BCRYPT_HASH, "ACTIVE", "ADMIN");

        List<String> indexes = jdbc.queryForList("""
                SELECT indexname FROM pg_indexes
                 WHERE schemaname = 'public'
                   AND tablename IN ('admin_user', 'admin_refresh_token', 'admin_audit_event')
                """, String.class);
        assertThat(indexes).contains(
                "ix_admin_user_status", "ix_admin_refresh_token_admin",
                "ix_admin_refresh_token_family", "ix_admin_audit_actor",
                "ix_admin_audit_occurred", "ix_admin_audit_correlation");

        assertThatThrownBy(() -> insertAdmin(UUID.randomUUID(), " Admin@example.com ",
                VALID_BCRYPT_HASH, "ACTIVE", "ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAdmin(UUID.randomUUID(), "ADMIN@example.com",
                VALID_BCRYPT_HASH, "ACTIVE", "ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAdmin(UUID.randomUUID(), "bad-hash@example.com",
                "not-a-bcrypt-hash", "ACTIVE", "ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAdmin(UUID.randomUUID(), "bad-status@example.com",
                VALID_BCRYPT_HASH, "UNKNOWN", "ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAdmin(UUID.randomUUID(), "bad-role@example.com",
                VALID_BCRYPT_HASH, "ACTIVE", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> insertRefreshToken(adminId, "raw-token",
                Instant.now(), Instant.now().plusSeconds(3600)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRefreshToken(adminId, "a".repeat(43),
                Instant.now(), Instant.now().minusSeconds(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void preservesFinancialDataWhenAdministrativeSchemaIsAlreadyPresent() {
        UUID accountId = UUID.randomUUID();
        String email = "existing-financial-" + accountId + "@example.com";
        insertAccount(accountId, "67676767676", email, "ACTIVE", new BigDecimal("10000.00"));

        assertThat(jdbc.queryForObject(
                "SELECT balance FROM account WHERE id = ?", BigDecimal.class, accountId))
                .isEqualByComparingTo("10000.00");
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
        assertThatThrownBy(() -> insertAdmin(UUID.randomUUID(),
                "invalid-hash-" + UUID.randomUUID() + "@example.com", passwordHash,
                "ACTIVE", "ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static void insertAdmin(UUID id, String email, String passwordHash,
            String status, String role) {
        jdbc.update("""
                INSERT INTO admin_user (id, email, password_hash, status, role, created_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, id, email, passwordHash, status, role);
    }

    private static void insertRefreshToken(UUID adminId, String tokenHash,
            Instant createdAt, Instant expiresAt) {
        jdbc.update("""
                INSERT INTO admin_refresh_token
                    (id, token_hash, admin_user_id, family_id, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), tokenHash, adminId, UUID.randomUUID(),
                Timestamp.from(createdAt), Timestamp.from(expiresAt));
    }

    private static void insertAccount(UUID id, String cpf, String email, String status) {
        insertAccount(id, cpf, email, status, new BigDecimal("10000.00"));
    }

    private static void insertAccount(UUID id, String cpf, String email, String status, BigDecimal balance) {
        jdbc.update("""
                INSERT INTO account
                    (id, name, cpf, email, password_hash, balance, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "Investidor PostgreSQL", cpf, email, "hash-for-schema-test", balance, status,
                Timestamp.from(Instant.now()));
    }
}
