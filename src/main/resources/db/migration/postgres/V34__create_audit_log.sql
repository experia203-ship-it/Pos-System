CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64),
    target_id BIGINT,
    performed_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    performed_by_name VARCHAR(50) NOT NULL,
    details VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_log_action ON audit_log(action);
