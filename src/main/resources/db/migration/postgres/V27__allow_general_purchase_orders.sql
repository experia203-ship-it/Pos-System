ALTER TABLE purchase_order
    ALTER COLUMN vendor_id DROP NOT NULL;

ALTER TABLE purchase_order_item
    ALTER COLUMN product_id DROP NOT NULL;
