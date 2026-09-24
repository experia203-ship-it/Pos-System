ALTER TABLE settings ADD COLUMN is_licensed INTEGER DEFAULT 0;
ALTER TABLE settings ADD COLUMN trial_ends_at TIMESTAMP;
ALTER TABLE settings ADD COLUMN last_accessed_at TIMESTAMP;
