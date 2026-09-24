CREATE TABLE shift_session (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    starting_float NUMERIC(10,2) NOT NULL CHECK (starting_float >= 0),
    expected_cash NUMERIC(10,2),
    counted_cash NUMERIC(10,2) CHECK (counted_cash >= 0),
    variance NUMERIC(10,2) GENERATED ALWAYS AS (counted_cash - expected_cash) STORED
);

CREATE TABLE cash_drawer_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    shift_id INTEGER REFERENCES shift_session(id),
    event_type VARCHAR(50) NOT NULL,
    amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE orders ADD COLUMN shift_id INTEGER REFERENCES shift_session(id);
