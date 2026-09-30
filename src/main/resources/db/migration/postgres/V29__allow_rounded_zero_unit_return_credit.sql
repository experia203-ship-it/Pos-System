ALTER TABLE purchase_return_item
    DROP CONSTRAINT purchase_return_item_unit_credit_check;

ALTER TABLE purchase_return_item
    ADD CONSTRAINT purchase_return_item_unit_credit_nonnegative_check CHECK (unit_credit >= 0);
