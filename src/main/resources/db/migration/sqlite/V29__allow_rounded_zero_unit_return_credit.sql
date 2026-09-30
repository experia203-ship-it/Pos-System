CREATE TABLE purchase_return_item_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_return_id INTEGER NOT NULL REFERENCES purchase_return(id) ON DELETE CASCADE,
    purchase_order_item_id INTEGER NOT NULL REFERENCES purchase_order_item(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_credit NUMERIC(10,2) NOT NULL CHECK (unit_credit >= 0),
    credit_total NUMERIC(10,2) NOT NULL CHECK (credit_total > 0)
);

INSERT INTO purchase_return_item_new (id, purchase_return_id, purchase_order_item_id, quantity, unit_credit, credit_total)
SELECT id, purchase_return_id, purchase_order_item_id, quantity, unit_credit, credit_total
FROM purchase_return_item;

DROP TABLE purchase_return_item;
ALTER TABLE purchase_return_item_new RENAME TO purchase_return_item;

CREATE INDEX idx_purchase_return_item_order_item ON purchase_return_item(purchase_order_item_id);
