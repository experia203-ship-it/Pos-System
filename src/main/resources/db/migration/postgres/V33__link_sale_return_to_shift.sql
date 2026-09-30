ALTER TABLE sale_return
    ADD COLUMN shift_id BIGINT;

ALTER TABLE sale_return
    ADD CONSTRAINT fk_sale_return_shift FOREIGN KEY (shift_id) REFERENCES shift_session(id);

CREATE INDEX idx_sale_return_shift ON sale_return(shift_id);
