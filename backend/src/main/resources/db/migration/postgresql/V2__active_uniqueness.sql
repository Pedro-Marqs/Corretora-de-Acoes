CREATE UNIQUE INDEX uq_account_active_cpf ON account (cpf) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_account_active_email ON account (LOWER(email)) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_account_broker_active ON account_broker (account_id, broker_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_asset_ticker_market ON asset (LOWER(ticker), market);
