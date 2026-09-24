alter table settings
add column is_licensed boolean default false,
add column trial_ends_at timestamp ,
add column last_accessed_at timestamp;