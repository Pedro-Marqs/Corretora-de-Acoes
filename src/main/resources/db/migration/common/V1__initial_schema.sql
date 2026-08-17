CREATE TABLE account (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    inactivated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_account_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT ck_account_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE broker (
    id UUID PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    corporate_name VARCHAR(200) NOT NULL,
    trade_name VARCHAR(200) NOT NULL,
    registration_status VARCHAR(40) NOT NULL,
    cvm_category VARCHAR(20) NOT NULL,
    postal_code VARCHAR(8) NOT NULL,
    street VARCHAR(200) NOT NULL,
    number VARCHAR(30) NOT NULL,
    complement VARCHAR(100),
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE account_broker (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    broker_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    associated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    removed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_account_broker_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_account_broker_broker FOREIGN KEY (broker_id) REFERENCES broker (id),
    CONSTRAINT uq_account_broker_id_account UNIQUE (id, account_id),
    CONSTRAINT ck_account_broker_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE asset (
    id UUID PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    market VARCHAR(8) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    CONSTRAINT ck_asset_market CHECK (market IN ('BR', 'US')),
    CONSTRAINT ck_asset_currency CHECK (currency IN ('BRL', 'USD')),
    CONSTRAINT ck_asset_market_currency CHECK ((market = 'BR' AND currency = 'BRL') OR (market = 'US' AND currency = 'USD'))
);

CREATE TABLE quote (
    asset_id UUID PRIMARY KEY,
    price NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    quoted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_quote_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT ck_quote_price_positive CHECK (price > 0),
    CONSTRAINT ck_quote_currency CHECK (currency IN ('BRL', 'USD'))
);

CREATE TABLE exchange_rate (
    currency_pair VARCHAR(7) PRIMARY KEY,
    rate NUMERIC(19, 2) NOT NULL,
    quoted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_exchange_rate_pair CHECK (currency_pair = 'USD/BRL'),
    CONSTRAINT ck_exchange_rate_positive CHECK (rate > 0)
);

CREATE TABLE position (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    account_broker_id UUID NOT NULL,
    asset_id UUID NOT NULL,
    quantity BIGINT NOT NULL,
    average_price NUMERIC(19, 2) NOT NULL,
    total_cost NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_position_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_position_account_broker FOREIGN KEY (account_broker_id, account_id)
        REFERENCES account_broker (id, account_id),
    CONSTRAINT fk_position_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT uq_position_account_broker_asset UNIQUE (account_id, account_broker_id, asset_id),
    CONSTRAINT ck_position_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT ck_position_average_price_non_negative CHECK (average_price >= 0),
    CONSTRAINT ck_position_total_cost_non_negative CHECK (total_cost >= 0)
);

CREATE TABLE movement (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    movement_type VARCHAR(24) NOT NULL,
    ticker VARCHAR(20),
    market VARCHAR(8),
    quote_price NUMERIC(19, 2),
    quantity BIGINT,
    total_amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    broker_name VARCHAR(200),
    origin_broker_name VARCHAR(200),
    destination_broker_name VARCHAR(200),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    remaining_balance NUMERIC(19, 2) NOT NULL,
    realized_result NUMERIC(19, 2),
    CONSTRAINT fk_movement_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT uq_movement_id_account UNIQUE (id, account_id),
    CONSTRAINT ck_movement_type CHECK (movement_type IN ('INITIAL_BALANCE', 'DEPOSIT', 'PURCHASE', 'SALE', 'TRANSFER')),
    CONSTRAINT ck_movement_quantity_positive CHECK (quantity IS NULL OR quantity > 0),
    CONSTRAINT ck_movement_total_non_negative CHECK (total_amount >= 0),
    CONSTRAINT ck_movement_remaining_balance_non_negative CHECK (remaining_balance >= 0),
    CONSTRAINT ck_movement_currency CHECK (currency IN ('BRL', 'USD')),
    CONSTRAINT ck_movement_market CHECK (market IS NULL OR market IN ('BR', 'US'))
);

CREATE TABLE patrimonial_point (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    movement_id UUID NOT NULL UNIQUE,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    patrimony_brl NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_patrimonial_point_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_patrimonial_point_movement FOREIGN KEY (movement_id, account_id)
        REFERENCES movement (id, account_id),
    CONSTRAINT ck_patrimonial_point_non_negative CHECK (patrimony_brl >= 0)
);

CREATE INDEX ix_position_account ON position (account_id);
CREATE INDEX ix_movement_account_occurred ON movement (account_id, occurred_at DESC);
CREATE INDEX ix_movement_account_filters ON movement (account_id, movement_type, ticker, market, occurred_at DESC);
CREATE INDEX ix_patrimonial_point_account_recorded ON patrimonial_point (account_id, recorded_at);

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INTEGER NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);
