alter table products
add column reorder_point bigint default 0 check(reorder_point>=0);