alter table orders add column customer_id bigint not null default 1;

alter table orders add constraint fk_customer foreign key (customer_id) references customer(id);


alter table orders alter column customer_id drop default;


alter table orders add column paid Numeric(10,2) not null default 0 check(paid>=0);

alter table orders add column remaining numeric(10,2) not null generated always as (total-paid) stored;