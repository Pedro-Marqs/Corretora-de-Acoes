ALTER TABLE movement DROP CONSTRAINT ck_movement_purchase_financial_inputs;

ALTER TABLE movement ADD CONSTRAINT ck_movement_trade_financial_inputs CHECK (
    (movement_type IN ('PURCHASE', 'SALE')
        AND unit_price_brl IS NOT NULL AND unit_price_brl > 0
        AND ((market = 'BR' AND usd_brl_rate IS NULL)
          OR (market = 'US' AND usd_brl_rate IS NOT NULL AND usd_brl_rate > 0)))
 OR (movement_type NOT IN ('PURCHASE', 'SALE')
        AND unit_price_brl IS NULL AND usd_brl_rate IS NULL)
);
