CREATE TABLE orders_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    user_name VARCHAR(50) NOT NULL,
    total NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (total >= 0),
    revenue NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    discount NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    customer_id INTEGER REFERENCES customer(id),
    paid NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (paid >= 0),
    remaining NUMERIC(10,2) NOT NULL GENERATED ALWAYS AS (total - paid) STORED,
    order_number VARCHAR(20)
);
INSERT INTO orders_new (
    id, user_id, user_name, total, revenue, created_at, discount,
    customer_id, paid, order_number
) SELECT id, user_id, user_name, total, revenue, created_at, discount,
    customer_id, paid, order_number FROM orders;
DROP TABLE orders;
ALTER TABLE orders_new RENAME TO orders;
CREATE UNIQUE INDEX orders_order_number_unique ON orders(order_number);
