package com.projeto.gestao.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchemaMigrationTests {

    private static final String PASSWORD_HASH = "hash-for-schema-test";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayCreatesDomainAndSpringSessionTables() {
        List<String> tables = jdbc.queryForList("""
                SELECT LOWER(table_name)
                  FROM information_schema.tables
                 WHERE table_schema = 'PUBLIC'
                """, String.class);

        assertThat(tables).contains(
                "account", "broker", "account_broker", "asset", "quote",
                "exchange_rate", "position", "movement", "patrimonial_point",
                "spring_session", "spring_session_attributes", "flyway_schema_history");
    }

    @Test
    void rejectsNegativeBalance() {
        assertThatThrownBy(() -> insertAccount(UUID.randomUUID(), "11111111111", "negative@example.com",
                new BigDecimal("-0.01"), "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativePositionQuantity() {
        UUID accountId = UUID.randomUUID();
        UUID accountBrokerId = createAccountBroker(accountId);
        UUID assetId = createAsset("NEG3");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO position
                    (id, account_id, account_broker_id, asset_id, quantity, average_price, total_cost)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), accountId, accountBrokerId, assetId, -1L,
                new BigDecimal("10.00"), new BigDecimal("10.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateActiveAccountButAllowsHistoricalAccount() {
        insertAccount(UUID.randomUUID(), "22222222222", "unique@example.com",
                new BigDecimal("10000.00"), "ACTIVE");

        assertThatThrownBy(() -> insertAccount(UUID.randomUUID(), "22222222222", "other@example.com",
                new BigDecimal("10000.00"), "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertAccount(UUID.randomUUID(), "22222222222", "unique@example.com",
                new BigDecimal("10000.00"), "INACTIVE");
    }

    @Test
    void rejectsDuplicateActiveEmailIgnoringCase() {
        insertAccount(UUID.randomUUID(), "77777777777", "Investor@Example.com",
                new BigDecimal("10000.00"), "ACTIVE");

        assertThatThrownBy(() -> insertAccount(UUID.randomUUID(), "88888888888", "investor@example.com",
                new BigDecimal("10000.00"), "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateActiveBrokerAssociationButAllowsHistoricalAssociation() {
        UUID accountId = UUID.randomUUID();
        insertAccount(accountId, "33333333333", "broker@example.com", new BigDecimal("10000.00"), "ACTIVE");
        UUID brokerId = createBroker();
        insertAccountBroker(UUID.randomUUID(), accountId, brokerId, "ACTIVE");

        assertThatThrownBy(() -> insertAccountBroker(UUID.randomUUID(), accountId, brokerId, "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertAccountBroker(UUID.randomUUID(), accountId, brokerId, "INACTIVE");
    }

    @Test
    void rejectsDuplicateAssetIgnoringTickerCase() {
        createAsset("PETR4");

        assertThatThrownBy(() -> createAsset("petr4"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicatePositionForAccountBrokerAndAsset() {
        UUID accountId = UUID.randomUUID();
        UUID associationId = createAccountBroker(accountId);
        UUID assetId = createAsset("DUPL3");
        insertPosition(accountId, associationId, assetId);

        assertThatThrownBy(() -> insertPosition(accountId, associationId, assetId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsPatrimonialPointWhoseAccountDiffersFromMovementAccount() {
        UUID movementAccountId = UUID.randomUUID();
        UUID pointAccountId = UUID.randomUUID();
        insertAccount(movementAccountId, "99999999999", "movement@example.com",
                new BigDecimal("10000.00"), "ACTIVE");
        insertAccount(pointAccountId, "10101010101", "point@example.com",
                new BigDecimal("10000.00"), "ACTIVE");
        UUID movementId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO movement
                    (id, account_id, movement_type, total_amount, currency, occurred_at, remaining_balance)
                VALUES (?, ?, 'DEPOSIT', 10.00, 'BRL', ?, 10010.00)
                """, movementId, movementAccountId, Timestamp.from(OffsetDateTime.now().toInstant()));

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO patrimonial_point
                    (id, account_id, movement_id, recorded_at, patrimony_brl)
                VALUES (?, ?, ?, ?, 10010.00)
                """, UUID.randomUUID(), pointAccountId, movementId,
                Timestamp.from(OffsetDateTime.now().toInstant())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID createAccountBroker(UUID accountId) {
        insertAccount(accountId, "44444444444", "position@example.com", new BigDecimal("10000.00"), "ACTIVE");
        UUID brokerId = createBroker();
        UUID associationId = UUID.randomUUID();
        insertAccountBroker(associationId, accountId, brokerId, "ACTIVE");
        return associationId;
    }

    private UUID createBroker() {
        UUID brokerId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO broker
                    (id, cnpj, corporate_name, trade_name, registration_status, cvm_category,
                     postal_code, street, number, district, city, state, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, brokerId, brokerId.toString().replace("-", "").substring(0, 14), "Corretora Teste S.A.",
                "Corretora Teste", "ACTIVE", "CTVM", "01001000", "Praça da Sé", "1", "Sé",
                "São Paulo", "SP", Timestamp.from(OffsetDateTime.now().toInstant()));
        return brokerId;
    }

    private UUID createAsset(String ticker) {
        UUID assetId = UUID.randomUUID();
        jdbc.update("INSERT INTO asset (id, ticker, name, market, currency) VALUES (?, ?, ?, 'BR', 'BRL')",
                assetId, ticker, "Ativo Teste");
        return assetId;
    }

    private void insertPosition(UUID accountId, UUID accountBrokerId, UUID assetId) {
        jdbc.update("""
                INSERT INTO position
                    (id, account_id, account_broker_id, asset_id, quantity, average_price, total_cost)
                VALUES (?, ?, ?, ?, 1, 10.00, 10.00)
                """, UUID.randomUUID(), accountId, accountBrokerId, assetId);
    }

    private void insertAccount(UUID id, String cpf, String email, BigDecimal balance, String status) {
        jdbc.update("""
                INSERT INTO account
                    (id, name, cpf, email, password_hash, balance, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "Investidor Teste", cpf, email, PASSWORD_HASH, balance, status,
                Timestamp.from(OffsetDateTime.now().toInstant()));
    }

    private void insertAccountBroker(UUID id, UUID accountId, UUID brokerId, String status) {
        jdbc.update("""
                INSERT INTO account_broker (id, account_id, broker_id, status, associated_at)
                VALUES (?, ?, ?, ?, ?)
                """, id, accountId, brokerId, status, Timestamp.from(OffsetDateTime.now().toInstant()));
    }
}
