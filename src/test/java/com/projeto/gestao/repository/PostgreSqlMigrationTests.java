package com.projeto.gestao.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrateSchema() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/common", "classpath:db/migration/postgresql")
                .load()
                .migrate();

        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    void migratesEmptyPostgreSqlDatabase() {
        Integer appliedMigrations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(appliedMigrations).isEqualTo(2);
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
