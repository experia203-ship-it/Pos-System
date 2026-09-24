ALTER TABLE purchase_order ADD COLUMN paid NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (paid >= 0);
ALTER TABLE purchase_order ADD COLUMN remaining NUMERIC(10,2) NOT NULL GENERATED ALWAYS AS (total - discount - paid) STORED;
