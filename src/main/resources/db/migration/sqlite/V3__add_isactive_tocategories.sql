ALTER TABLE category ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1;
CREATE INDEX index_category_is_active_true ON category(id) WHERE is_active = 1;
