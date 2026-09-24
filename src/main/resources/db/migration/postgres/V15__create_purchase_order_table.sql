
create table vendor(
    id bigserial primary key,
    name varchar(255) not null,
    location varchar(500),
    landline varchar(50) ,
    mobile varchar(50)


);

create table purchase_order(
id bigserial primary key,
user_id bigint not null,
    vendor_id bigint not null,
user_name varchar(50) not null,
total numeric(10,2) not null default 0.00 check (total>=0),
created_at timestamp default current_timestamp,
discount numeric(10,2) not null default 0.00,
constraint fk_user foreign key (user_id) references users(id),

constraint fk_vendor foreign key (vendor_id) references vendor(id)
);

create table purchase_order_item(
    id bigserial primary key ,
    order_id bigint not null,
    product_id bigint not null,
    product_name varchar(255) not null,
    product_purchase_price numeric (10,2) not null default 0.00,
    quantity int not null default 0 check (quantity>=0),
    sub_total numeric(10,2) generated always as ((quantity*product_purchase_price)-sub_discount ) stored,
    sub_discount numeric(10,2) not null default 0.00,

    constraint fk_order foreign key (order_id) references purchase_order(id),
    constraint fk_product foreign key (product_id) references products(id)




);