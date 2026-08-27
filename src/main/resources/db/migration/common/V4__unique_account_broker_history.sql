ALTER TABLE account_broker
    ADD CONSTRAINT uq_account_broker_account_broker UNIQUE (account_id, broker_id);
