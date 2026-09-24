CREATE TABLE vendor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(500),
    landline VARCHAR(50),
    mobile VARCHAR(50)
);

CREATE TABLE purchase_order (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    vendor_id INTEGER NOT NULL REFERENCES vendor(id),
    user_name VARCHAR(50) NOT NULL,
    total NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (total >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    discount NUMERIC(10,2) NOT NULL DEFAULT 0.00
);

CREATE TABLE purchase_order_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL REFERENCES purchase_order(id),
    product_id INTEGER NOT NULL REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    product_purchase_price NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    sub_total NUMERIC(10,2) GENERATED ALWAYS AS ((quantity * product_purchase_price) - sub_discount) STORED,
    sub_discount NUMERIC(10,2) NOT NULL DEFAULT 0.00
);
