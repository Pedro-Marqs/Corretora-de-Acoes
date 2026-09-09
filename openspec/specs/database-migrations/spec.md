# database-migrations Specification

## Purpose
Este capability define a aplicação controlada e reproduzível do esquema relacional do backend por changelogs versionados, preservando as diferenças necessárias entre PostgreSQL e H2.

## Requirements

### Requirement: Liquibase SHALL be the single schema migration mechanism

O backend SHALL use the Liquibase changelog mestre `classpath:db/changelog/db.changelog-master.xml` as its migration entry point, with migrations enabled and automatic ORM schema creation disabled in favor of validation.

#### Scenario: Application starts against a compatible database
- **WHEN** the backend starts with a configured PostgreSQL or H2 datasource
- **THEN** Liquibase reads the configured master changelog and Hibernate validates the resulting schema without creating or altering tables automatically

#### Scenario: Legacy Flyway configuration is absent
- **WHEN** the backend configuration is inspected after the migration
- **THEN** it contains no `spring.flyway.locations` setting and the backend has no Flyway runtime dependency

### Requirement: The master changelog SHALL reproduce all current migrations in order

The master changelog SHALL include ordered Liquibase XML changeSets representing every current migration from `db/migration`: the initial schema, the database-specific active-uniqueness migration, and migrations V3 through V8. The resulting schema SHALL preserve tables, columns, constraints, indexes, defaults, data backfills, and database-specific behavior of the reference migrations.

#### Scenario: Empty PostgreSQL database
- **WHEN** Liquibase runs against an empty PostgreSQL database
- **THEN** it applies the complete ordered set once, creates the application and Spring Session tables, and creates the PostgreSQL partial/expression uniqueness indexes

#### Scenario: Empty H2 test database
- **WHEN** Liquibase runs against an empty H2 database
- **THEN** it applies the complete compatible ordered set once and enforces active-account, active-broker, and normalized-asset uniqueness through the H2-compatible representation

#### Scenario: Existing schema is already current
- **WHEN** the backend starts again against a database whose Liquibase changeSets are recorded as applied
- **THEN** no changeSet is executed a second time and application startup remains successful

### Requirement: Migration history and locking SHALL be durable and observable

Liquibase SHALL record applied changeSets in `DATABASECHANGELOG` and coordinate execution with `DATABASECHANGELOGLOCK`, releasing the lock after successful or failed execution so a subsequent startup can proceed.

#### Scenario: Successful migration
- **WHEN** all master changeSets complete successfully
- **THEN** `DATABASECHANGELOG` contains one record for each applied changeSet and `DATABASECHANGELOGLOCK` reports an available lock

#### Scenario: Migration failure
- **WHEN** a changeSet cannot be applied
- **THEN** startup fails without being reported as a successful migration, the failure remains diagnosable from the application error, and the Liquibase lock is not left permanently held

### Requirement: The legacy migration directory SHALL be removed only after validation

The conversion SHALL retain `backend/src/main/resources/db/migration/` as the reference during authoring and comparison, and SHALL remove it only after the XML changelogs have been checked for completeness and the required Maven and PostgreSQL validations have passed.

#### Scenario: Conversion is incomplete
- **WHEN** any reference migration is not represented or validation has not passed
- **THEN** the legacy directory remains present and the change is not considered complete

#### Scenario: Conversion is validated
- **WHEN** all reference migrations are represented, `cd backend && .\mvnw.cmd test` passes, and a PostgreSQL startup confirms the Liquibase tables and schema
- **THEN** the legacy migration directory may be removed without changing the master changelog entry point
