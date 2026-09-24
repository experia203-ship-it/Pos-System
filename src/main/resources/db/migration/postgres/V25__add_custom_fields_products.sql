alter table products
add column custom_fields jsonb default '{}'::jsonb;