create table shift_session(
    id bigserial primary key,
    user_id  bigint not null,
    status varchar(50) not null,
    start_time timestamp default current_timestamp not null,
    end_time timestamp,
    starting_float numeric(10,2) check (starting_float>=0) not null,
    expected_cash numeric(10,2),
    counted_cash numeric(10,2) check (counted_cash>=0),
    variance numeric(10,2) generated always as (counted_cash-expected_cash) stored,
     constraint fk_user2 foreign key (user_id) references users(id)

);
create table cash_drawer_event(
    id bigserial primary key,
    shift_id  bigint,
    event_type varchar(50) not null,
    amount numeric(10,2) not null check (amount>0) ,
    reason varchar(255),
    created_at timestamp default current_timestamp,

    constraint fk_shift foreign key (shift_id) references shift_session(id)

);

alter table orders

add column shift_id bigint;
alter table orders

add constraint fk_shift2 foreign key (shift_id) references shift_session(id);
