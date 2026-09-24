create table users
(
    id bigserial primary key,
    name varchar(50) not null,
    email varchar(255) not null unique,
    password varchar(255) not null,
    created_at timestamp default current_timestamp
);

create table roles(
    id bigserial primary key,
   name varchar(50) unique not null
);
insert into roles (name) VALUES ('ROLE_ADMIN') , ('ROLE_USER');

create table user_roles(
    user_id bigint not null references users(id),
    role_id bigint not null references roles(id),

    primary key( user_id,role_id)

);

create table category(
    id bigserial primary key,
    name varchar(50) not null,
    description varchar(255)



);
create table products(
    id bigserial primary key,
    category_id bigint ,
    name varchar(255) not null unique ,
    part_number varchar(50) unique not null,
    description varchar(255) not null,
    selling_price numeric(10,2) check (selling_price>0),
    purchase_price numeric(10,2) check (purchase_price>0),
    stock bigint default 0 check (stock>=0),
    created_at timestamp default current_timestamp,
    updated_at timestamp,

   constraint fk_category foreign key (category_id) references category(id) on delete SET NULL
);

create table orders(
  id bigserial primary key,
    user_id bigint not null,
    user_name varchar(50) not null,
    total numeric(10,2) not null default 0.00 check (total>=0),
    revenue numeric(10,2) not null default 0.00 ,
    created_at timestamp default current_timestamp,
    discount numeric(5,2) not null default 0.00,
constraint fk_user foreign key (user_id) references users(id)


);

create table order_items(
id bigserial primary key ,
    order_id bigint not null,
    product_id bigint not null,
    product_name varchar(255) not null,
    product_selling_price numeric(10,2) not null default 0.00,
    product_purchase_price numeric (10,2) not null default 0.00,
    quantity int not null default 0 check (quantity>=0),
    sub_total numeric(10,2) generated always as ((quantity*product_selling_price)-sub_discount ) stored,
    sub_revenue numeric(10,2) generated always as ( ((product_selling_price-product_purchase_price)*quantity)-sub_discount) stored,
    sub_discount numeric(5,2) not null default 0.00,

    constraint fk_order foreign key(order_id)  references orders(id) on delete cascade ,
    constraint fk_product foreign key(product_id) references products(id)




);

