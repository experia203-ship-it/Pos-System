create extension if not exists pg_trgm;

create index idx_name_products on products using gin (name gin_trgm_ops);
create index idx_description_products on products using gin (description gin_trgm_ops);