ALTER TABLE products ADD COLUMN reorder_point INTEGER DEFAULT 0 CHECK (reorder_point >= 0);
