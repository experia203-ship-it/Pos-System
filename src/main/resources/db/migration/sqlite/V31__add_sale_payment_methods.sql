ALTER TABLE orders
    ADD COLUMN payment_method VARCHAR(32) NOT NULL DEFAULT 'CASH';

ALTER TABLE orders
    ADD COLUMN payment_reference VARCHAR(100);

ALTER TABLE orders
    ADD COLUMN cash_received NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (cash_received >= 0);

ALTER TABLE orders
    ADD COLUMN cash_change NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (cash_change >= 0);

UPDATE orders SET cash_received = paid;
