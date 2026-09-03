ALTER TABLE movement ADD COLUMN unit_price_brl NUMERIC(19, 2);
ALTER TABLE movement ADD COLUMN usd_brl_rate NUMERIC(19, 2);

UPDATE movement
   SET unit_price_brl = total_amount / quantity,
       usd_brl_rate = CASE
           WHEN market = 'US' THEN (total_amount / quantity) / quote_price
           ELSE NULL
       END
 WHERE movement_type = 'PURCHASE';

ALTER TABLE movement ADD CONSTRAINT ck_movement_purchase_financial_inputs CHECK (
    (movement_type = 'PURCHASE' AND unit_price_brl IS NOT NULL AND unit_price_brl > 0
        AND ((market = 'BR' AND usd_brl_rate IS NULL)
          OR (market = 'US' AND usd_brl_rate IS NOT NULL AND usd_brl_rate > 0)))
 OR (movement_type <> 'PURCHASE' AND unit_price_brl IS NULL AND usd_brl_rate IS NULL)
);
