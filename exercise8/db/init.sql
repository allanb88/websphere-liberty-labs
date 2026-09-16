-- =============================================================
-- Products database schema and seed data
-- Auto-executed by the official postgres Docker image on first
-- container start (placed in /docker-entrypoint-initdb.d/)
-- =============================================================

-- Products table
CREATE TABLE products (
    id    SERIAL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0)
);

-- Seed data — 5 sample products
INSERT INTO products (name, price, stock) VALUES
    ('Wireless Keyboard',  49.99, 120),
    ('USB-C Hub',          34.95,  85),
    ('Mechanical Mouse',   59.00,  60),
    ('Monitor Stand',      29.99,  40),
    ('Laptop Sleeve 15"',  19.50, 200);
