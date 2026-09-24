create table customer(

    id bigserial primary key,

    name varchar(50) not null,

    location varchar(1000) not null,

    shipping_company varchar(50) not null

);


create table customer_phone(

    id bigserial primary key,
    customer_id bigint not null,
    type varchar(50) not null,
    is_primary boolean not null default false,
    phone_number varchar(50) not null,
constraint fk_customer foreign key (customer_id) references customer(id)
);