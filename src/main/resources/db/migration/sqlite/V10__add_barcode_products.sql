ALTER TABLE products ADD COLUMN barcode VARCHAR(50);
CREATE UNIQUE INDEX products_barcode_unique ON products(barcode);
