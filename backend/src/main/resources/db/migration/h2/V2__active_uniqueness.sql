ALTER TABLE account ADD COLUMN active_cpf VARCHAR(11)
    GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN cpf ELSE NULL END);
ALTER TABLE account ADD COLUMN active_email VARCHAR(254)
    GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN LOWER(email) ELSE NULL END);
ALTER TABLE account_broker ADD COLUMN active_broker_id UUID
    GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN broker_id ELSE NULL END);
ALTER TABLE asset ADD COLUMN normalized_ticker VARCHAR(20)
    GENERATED ALWAYS AS (LOWER(ticker));

CREATE UNIQUE INDEX uq_account_active_cpf ON account (active_cpf);
CREATE UNIQUE INDEX uq_account_active_email ON account (active_email);
CREATE UNIQUE INDEX uq_account_broker_active ON account_broker (account_id, active_broker_id);
CREATE UNIQUE INDEX uq_asset_ticker_market ON asset (normalized_ticker, market);
