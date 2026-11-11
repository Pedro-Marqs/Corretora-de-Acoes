package com.projeto.gestao.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchemaMigrationTests {

    private static final String PASSWORD_HASH = "hash-for-schema-test";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSource dataSource;

    private DataSource rollbackDataSource;

    @AfterEach
    void closeRollbackDataSource() throws Exception {
        if (rollbackDataSource instanceof JdbcDataSource h2) {
            h2.getConnection().close();
        }
    }

    @Test
    void marketCacheMigrationBackfillsAndConstrainsCatalogMetadata() {
        UUID assetId = createAsset("CACHE3");
        jdbc.update("""
                INSERT INTO quote (asset_id, price, currency, quoted_at, collected_at)
                VALUES (?, 12.34, 'BRL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, assetId);
        jdbc.update("""
                INSERT INTO exchange_rate (currency_pair, rate, quoted_at, collected_at)
                VALUES ('USD/BRL', 5.20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        assertThat(jdbc.queryForObject(
                "SELECT asset_type || ':' || status FROM asset WHERE id = ?", String.class, assetId))
                .isEqualTo("STOCK:ACTIVE");
        assertThat(jdbc.queryForObject(
                "SELECT source FROM quote WHERE asset_id = ?", String.class, assetId)).isEqualTo("LEGACY");
        assertThat(jdbc.queryForObject(
                "SELECT source FROM exchange_rate WHERE currency_pair = 'USD/BRL'", String.class))
                .isEqualTo("LEGACY");
        assertThatThrownBy(() -> jdbc.update("UPDATE asset SET status = 'UNKNOWN' WHERE id = ?", assetId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void liquibaseCreatesDomainAndSpringSessionTables() {
        List<String> tables = jdbc.queryForList("""
                SELECT LOWER(table_name)
                  FROM information_schema.tables
                 WHERE table_schema = 'PUBLIC'
                """, String.class);

        assertThat(tables).contains(
                "account", "broker", "account_broker", "asset", "quote",
                "exchange_rate", "position", "movement", "patrimonial_point",
                "refresh_token", "admin_user", "admin_refresh_token", "admin_audit_event",
                "spring_session", "spring_session_attributes",
                "databasechangelog", "databasechangeloglock");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = 'v11-refresh-token'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = 'v12-admin-foundation'", Integer.class))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {" Admin@example.com ", "admin@example.com ", "ADMIN@example.com"})
    void h2RejectsAdministrativeEmailThatIsNotCanonical(String email) {
        assertThatThrownBy(() -> insertAdminUser(email, passwordEncoder.encode("SenhaAdmin1!"),
                "ACTIVE", "ADMIN", "a".repeat(43), OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void h2RejectsInvalidAdministrativeSensitiveFields() {
        assertThatThrownBy(() -> insertAdminUser(
                "admin@example.com", "not-a-bcrypt-hash", "ACTIVE", "ADMIN",
                "a".repeat(43), OffsetDateTime.now(), OffsetDateTime.now().plusHours(1)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> insertAdminUser(
                "admin@example.com", passwordEncoder.encode("SenhaAdmin1!"), "ACTIVE", "ADMIN",
                "raw-token", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
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
    void h2RejectsStructurallyInvalidBcryptPasswordHashes(String passwordHash) {
        assertThatThrownBy(() -> insertAdminUser(
                "admin@example.com", passwordHash, "ACTIVE", "ADMIN",
                "a".repeat(43), OffsetDateTime.now(), OffsetDateTime.now().plusHours(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void h2ExposesAdministrativeIndexesAndPreservesFinancialDataWhenLiquibaseIsReapplied()
            throws Exception {
        UUID accountId = UUID.randomUUID();
        insertAccount(accountId, "45454545454", "migration-existing@example.com",
                new BigDecimal("10000.00"), "ACTIVE");

        List<String> indexes = jdbc.queryForList("""
                SELECT LOWER(index_name) FROM information_schema.indexes
                 WHERE table_name IN ('ADMIN_USER', 'ADMIN_REFRESH_TOKEN', 'ADMIN_AUDIT_EVENT')
                """, String.class);
        assertThat(indexes).contains(
                "ix_admin_user_status", "ix_admin_refresh_token_admin",
                "ix_admin_refresh_token_family", "ix_admin_audit_actor",
                "ix_admin_audit_occurred", "ix_admin_audit_correlation");

        try (var connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
        }

        assertThat(jdbc.queryForObject(
                "SELECT balance FROM account WHERE id = ?", BigDecimal.class, accountId))
                .isEqualByComparingTo("10000.00");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = 'v12-admin-foundation'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void refreshTokenMigrationRollsBackOnFreshH2Database() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:liquibase-rollback-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        rollbackDataSource = dataSource;

        try (var connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
            assertThat(tableExists(connection, "REFRESH_TOKEN")).isTrue();
            assertThat(tableExists(connection, "ADMIN_USER")).isTrue();
            insertFinancialAccount(connection, UUID.randomUUID());

            liquibase.rollback(5, new Contexts(), new LabelExpression());
            assertThat(tableExists(connection, "REFRESH_TOKEN")).isFalse();
            assertThat(tableExists(connection, "ADMIN_USER")).isFalse();
            assertThat(tableExists(connection, "ADMIN_AUDIT_EVENT")).isFalse();
            assertThat(tableExists(connection, "ACCOUNT")).isTrue();
        }
    }

    private void insertAdminUser(String email, String passwordHash, String status, String role,
            String tokenHash, OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        UUID adminId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO admin_user
                    (id, email, password_hash, status, role, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, adminId, email, passwordHash, status, role,
                Timestamp.from(createdAt.toInstant()));
        jdbc.update("""
                INSERT INTO admin_refresh_token
                    (id, token_hash, admin_user_id, family_id, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), tokenHash, adminId, UUID.randomUUID(),
                Timestamp.from(createdAt.toInstant()), Timestamp.from(expiresAt.toInstant()));
    }

    private void insertFinancialAccount(java.sql.Connection connection, UUID accountId)
            throws Exception {
        try (var statement = connection.prepareStatement("""
                INSERT INTO account
                    (id, name, cpf, email, password_hash, balance, status, created_at)
                VALUES (?, 'Existing investor', '78787878787', 'rollback-existing@example.com',
                        'hash-for-schema-test', 10000.00, 'ACTIVE', CURRENT_TIMESTAMP)
                """)) {
            statement.setObject(1, accountId);
            statement.executeUpdate();
        }
    }

    private boolean tableExists(java.sql.Connection connection, String tableName) throws Exception {
        try (var result = connection.getMetaData().getTables(null, "PUBLIC", tableName, null)) {
            return result.next();
        }
    }

    @Test
    void h2UsesTheH2ActiveUniquenessRepresentation() {
        List<String> columns = jdbc.queryForList("""
                SELECT LOWER(column_name) FROM information_schema.columns
                 WHERE table_name IN ('ACCOUNT', 'ACCOUNT_BROKER', 'ASSET')
                """, String.class);

        assertThat(columns).contains(
                "active_cpf", "active_email", "active_broker_id", "normalized_ticker");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = 'v2-active-uniqueness'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void patrimonialPointStoresCalculationInputs() {
        List<String> columns = jdbc.queryForList("""
                SELECT LOWER(column_name) FROM information_schema.columns
                 WHERE table_name = 'PATRIMONIAL_POINT'
                """, String.class);
        assertThat(columns).contains("balance_brl", "positions_value_brl", "usd_brl_rate");
    }

    @Test
    void movementStoresPurchaseConversionInputs() {
        List<String> columns = jdbc.queryForList("""
                SELECT LOWER(column_name) FROM information_schema.columns
                 WHERE table_name = 'MOVEMENT'
                """, String.class);
        assertThat(columns).contains("unit_price_brl", "usd_brl_rate");

        UUID accountId = UUID.randomUUID();
        insertAccount(accountId, "13131313131", "purchase-inputs@example.com",
                new BigDecimal("10000.00"), "ACTIVE");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO movement
                    (id, account_id, movement_type, ticker, market, quote_price, quantity,
                     total_amount, currency, broker_name, occurred_at, remaining_balance)
                VALUES (?, ?, 'PURCHASE', 'AAPL', 'US', 10.00, 1,
                        50.00, 'USD', 'Broker', ?, 9950.00)
                """, UUID.randomUUID(), accountId,
                Timestamp.from(OffsetDateTime.now().toInstant())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMovementWithFieldsInapplicableToItsType() {
        UUID accountId = UUID.randomUUID();
        insertAccount(accountId, "12121212121", "movement-fields@example.com",
                new BigDecimal("10000.00"), "ACTIVE");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO movement
                    (id, account_id, movement_type, ticker, total_amount, currency,
                     occurred_at, remaining_balance)
                VALUES (?, ?, 'DEPOSIT', 'PETR4', 10.00, 'BRL', ?, 10010.00)
                """, UUID.randomUUID(), accountId,
                Timestamp.from(OffsetDateTime.now().toInstant())))
                .isInstanceOf(DataIntegrityViolationException.class);
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
    void rejectsSecondHistoricalBrokerAssociationForSameAccountAndBroker() {
        UUID accountId = UUID.randomUUID();
        insertAccount(accountId, "33333333333", "broker@example.com", new BigDecimal("10000.00"), "ACTIVE");
        UUID brokerId = createBroker();
        insertAccountBroker(UUID.randomUUID(), accountId, brokerId, "ACTIVE");

        assertThatThrownBy(() -> insertAccountBroker(UUID.randomUUID(), accountId, brokerId, "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> insertAccountBroker(UUID.randomUUID(), accountId, brokerId, "INACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);
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
                    (id, account_id, movement_id, recorded_at,
                     balance_brl, positions_value_brl, patrimony_brl)
                VALUES (?, ?, ?, ?, 10010.00, 0.00, 10010.00)
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
