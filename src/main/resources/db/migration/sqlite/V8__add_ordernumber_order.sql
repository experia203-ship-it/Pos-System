ALTER TABLE orders ADD COLUMN order_number VARCHAR(20);
CREATE UNIQUE INDEX orders_order_number_unique ON orders(order_number);
