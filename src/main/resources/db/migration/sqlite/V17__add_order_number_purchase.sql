ALTER TABLE purchase_order ADD COLUMN order_number VARCHAR(20);
CREATE UNIQUE INDEX purchase_order_number_unique ON purchase_order(order_number);
