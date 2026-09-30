ALTER TABLE sale_return
    ADD COLUMN shift_id INTEGER REFERENCES shift_session(id);

CREATE INDEX idx_sale_return_shift ON sale_return(shift_id);
