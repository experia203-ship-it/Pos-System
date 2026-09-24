CREATE TABLE order_items_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id INTEGER REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    product_selling_price NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    product_purchase_price NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    sub_total NUMERIC(10,2) GENERATED ALWAYS AS ((quantity * product_selling_price) - sub_discount) STORED,
    sub_revenue NUMERIC(10,2) GENERATED ALWAYS AS (((product_selling_price - product_purchase_price) * quantity) - sub_discount) STORED,
    sub_discount NUMERIC(5,2) NOT NULL DEFAULT 0.00
);
INSERT INTO order_items_new (
    id, order_id, product_id, product_name, product_selling_price,
    product_purchase_price, quantity, sub_discount
) SELECT id, order_id, product_id, product_name, product_selling_price,
    product_purchase_price, quantity, sub_discount FROM order_items;
DROP TABLE order_items;
ALTER TABLE order_items_new RENAME TO order_items;
