ALTER TABLE purchase_order
    ADD COLUMN vendor_credit NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (vendor_credit >= 0);

CREATE TABLE purchase_return (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_order_id INTEGER NOT NULL REFERENCES purchase_order(id),
    user_id INTEGER NOT NULL REFERENCES users(id),
    reason VARCHAR(255) NOT NULL,
    credit_total NUMERIC(10,2) NOT NULL CHECK (credit_total > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_return_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_return_id INTEGER NOT NULL REFERENCES purchase_return(id) ON DELETE CASCADE,
    purchase_order_item_id INTEGER NOT NULL REFERENCES purchase_order_item(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_credit NUMERIC(10,2) NOT NULL CHECK (unit_credit > 0),
    credit_total NUMERIC(10,2) NOT NULL CHECK (credit_total > 0)
);

CREATE INDEX idx_purchase_return_order ON purchase_return(purchase_order_id);
CREATE INDEX idx_purchase_return_item_order_item ON purchase_return_item(purchase_order_item_id);
