CREATE TABLE purchase_order_replacement (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    vendor_id INTEGER REFERENCES vendor(id),
    user_name VARCHAR(50) NOT NULL,
    total NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (total >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    discount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    paid NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (paid >= 0),
    remaining NUMERIC(10,2) NOT NULL GENERATED ALWAYS AS (total - discount - paid) STORED,
    order_number VARCHAR(20)
);

INSERT INTO purchase_order_replacement (id, user_id, vendor_id, user_name, total, created_at, discount, paid, order_number)
SELECT id, user_id, vendor_id, user_name, total, created_at, discount, paid, order_number
FROM purchase_order;

CREATE TABLE purchase_order_item_replacement (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL REFERENCES purchase_order_replacement(id),
    product_id INTEGER REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    product_purchase_price NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    sub_total NUMERIC(10,2) GENERATED ALWAYS AS ((quantity * product_purchase_price) - sub_discount) STORED,
    sub_discount NUMERIC(10,2) NOT NULL DEFAULT 0.00
);

INSERT INTO purchase_order_item_replacement
    (id, order_id, product_id, product_name, product_purchase_price, quantity, sub_discount)
SELECT id, order_id, product_id, product_name, product_purchase_price, quantity, sub_discount
FROM purchase_order_item;

DROP TABLE purchase_order_item;
DROP TABLE purchase_order;
ALTER TABLE purchase_order_replacement RENAME TO purchase_order;
ALTER TABLE purchase_order_item_replacement RENAME TO purchase_order_item;
CREATE UNIQUE INDEX purchase_order_number_unique ON purchase_order(order_number);
