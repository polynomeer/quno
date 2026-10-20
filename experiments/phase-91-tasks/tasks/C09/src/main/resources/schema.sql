CREATE TABLE IF NOT EXISTS orders (
    order_no VARCHAR(40) PRIMARY KEY,
    customer VARCHAR(80) NOT NULL,
    quantity INT NOT NULL
);
