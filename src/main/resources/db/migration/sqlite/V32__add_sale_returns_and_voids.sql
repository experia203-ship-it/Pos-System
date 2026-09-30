ALTER TABLE orders
    ADD COLUMN return_credit NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (return_credit >= 0);

ALTER TABLE orders
    ADD COLUMN refunded_total NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (refunded_total >= 0);

ALTER TABLE orders
    ADD COLUMN voided INTEGER NOT NULL DEFAULT 0;

CREATE TABLE sale_return (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL REFERENCES orders(id),
    user_id INTEGER NOT NULL REFERENCES users(id),
    return_type VARCHAR(16) NOT NULL CHECK (return_type IN ('RETURN', 'VOID')),
    reason VARCHAR(255) NOT NULL,
    credit_total NUMERIC(10,2) NOT NULL CHECK (credit_total >= 0),
    refund_total NUMERIC(10,2) NOT NULL CHECK (refund_total >= 0),
    payment_method VARCHAR(32) NOT NULL,
    payment_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sale_return_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_return_id INTEGER NOT NULL REFERENCES sale_return(id) ON DELETE CASCADE,
    order_item_id INTEGER NOT NULL REFERENCES order_items(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    restocked INTEGER NOT NULL DEFAULT 0,
    credit_total NUMERIC(10,2) NOT NULL CHECK (credit_total >= 0),
    unit_credit NUMERIC(10,2) NOT NULL CHECK (unit_credit >= 0)
);

CREATE INDEX idx_sale_return_order ON sale_return(order_id);
CREATE INDEX idx_sale_return_item_order_item ON sale_return_item(order_item_id);
