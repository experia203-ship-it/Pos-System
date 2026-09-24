
create table settings(
id int primary key check(id=1),

    company_name varchar(255) not null default('my company'),
    phone_number varchar(13) ,
    address varchar(255),
    tax_registration_number varchar(255),
    default_theme varchar(50) default('SYSTEM_DEFAULT'),
    print_size varchar(50) default('A5'),
    currency_symbol  varchar(50) default('EGP')
);