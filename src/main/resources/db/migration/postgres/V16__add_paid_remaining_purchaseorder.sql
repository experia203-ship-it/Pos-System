alter table purchase_order
add column paid numeric(10,2) not null check(paid>=0) default 0,
add column remaining numeric(10,2) not null generated always as (total-discount-paid) stored;