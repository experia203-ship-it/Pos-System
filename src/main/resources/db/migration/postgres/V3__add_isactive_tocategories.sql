alter table category
add column is_active boolean default true not null;

create index index_category_is_active_true on category(id)
    where is_active=true;