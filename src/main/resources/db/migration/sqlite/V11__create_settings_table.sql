CREATE TABLE settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    company_name VARCHAR(255) NOT NULL DEFAULT 'my company',
    phone_number VARCHAR(13),
    address VARCHAR(255),
    tax_registration_number VARCHAR(255),
    default_theme VARCHAR(50) DEFAULT 'SYSTEM_DEFAULT',
    print_size VARCHAR(50) DEFAULT 'A5',
    currency_symbol VARCHAR(50) DEFAULT 'EGP'
);
