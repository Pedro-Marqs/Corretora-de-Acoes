ALTER TABLE patrimonial_point ADD COLUMN balance_brl NUMERIC(19, 2);
ALTER TABLE patrimonial_point ADD COLUMN positions_value_brl NUMERIC(19, 2);
ALTER TABLE patrimonial_point ADD COLUMN usd_brl_rate NUMERIC(19, 2);

UPDATE patrimonial_point
   SET balance_brl = patrimony_brl,
       positions_value_brl = 0.00;

ALTER TABLE patrimonial_point ALTER COLUMN balance_brl SET NOT NULL;
ALTER TABLE patrimonial_point ALTER COLUMN positions_value_brl SET NOT NULL;

ALTER TABLE patrimonial_point ADD CONSTRAINT ck_patrimonial_point_balance_non_negative CHECK (balance_brl >= 0);
ALTER TABLE patrimonial_point ADD CONSTRAINT ck_patrimonial_point_positions_non_negative CHECK (positions_value_brl >= 0);
ALTER TABLE patrimonial_point ADD CONSTRAINT ck_patrimonial_point_rate_positive CHECK (usd_brl_rate IS NULL OR usd_brl_rate > 0);
ALTER TABLE patrimonial_point ADD CONSTRAINT ck_patrimonial_point_components CHECK (patrimony_brl = balance_brl + positions_value_brl);

ALTER TABLE movement ADD CONSTRAINT ck_movement_fields_by_type CHECK (
    (movement_type = 'INITIAL_BALANCE' AND total_amount > 0 AND currency = 'BRL'
        AND ticker IS NULL AND market IS NULL AND quote_price IS NULL AND quantity IS NULL
        AND broker_name IS NULL AND origin_broker_name IS NULL AND destination_broker_name IS NULL AND realized_result IS NULL)
 OR (movement_type = 'DEPOSIT' AND total_amount > 0 AND currency = 'BRL'
        AND ticker IS NULL AND market IS NULL AND quote_price IS NULL AND quantity IS NULL
        AND broker_name IS NULL AND origin_broker_name IS NULL AND destination_broker_name IS NULL AND realized_result IS NULL)
 OR (movement_type = 'PURCHASE' AND ticker IS NOT NULL AND market IS NOT NULL
        AND quote_price > 0 AND quantity > 0 AND total_amount > 0 AND broker_name IS NOT NULL
        AND origin_broker_name IS NULL AND destination_broker_name IS NULL AND realized_result IS NULL
        AND ((market = 'BR' AND currency = 'BRL') OR (market = 'US' AND currency = 'USD')))
 OR (movement_type = 'SALE' AND ticker IS NOT NULL AND market IS NOT NULL
        AND quote_price > 0 AND quantity > 0 AND total_amount > 0 AND broker_name IS NOT NULL
        AND origin_broker_name IS NULL AND destination_broker_name IS NULL AND realized_result IS NOT NULL
        AND ((market = 'BR' AND currency = 'BRL') OR (market = 'US' AND currency = 'USD')))
 OR (movement_type = 'TRANSFER' AND ticker IS NOT NULL AND market IS NOT NULL
        AND quote_price IS NULL AND quantity > 0 AND total_amount > 0 AND broker_name IS NULL
        AND origin_broker_name IS NOT NULL AND destination_broker_name IS NOT NULL
        AND origin_broker_name <> destination_broker_name AND realized_result IS NULL
        AND ((market = 'BR' AND currency = 'BRL') OR (market = 'US' AND currency = 'USD')))
);
