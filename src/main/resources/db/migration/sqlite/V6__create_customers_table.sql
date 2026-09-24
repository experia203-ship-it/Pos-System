CREATE TABLE customer (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL,
    location VARCHAR(1000) NOT NULL,
    shipping_company VARCHAR(50) NOT NULL
);

CREATE TABLE customer_phone (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL REFERENCES customer(id),
    type VARCHAR(50) NOT NULL,
    is_primary INTEGER NOT NULL DEFAULT 0,
    phone_number VARCHAR(50) NOT NULL
);
