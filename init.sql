CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price INTEGER NOT NULL,
    stock INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    total_price INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    failed_at TIMESTAMP,
    expires_at TIMESTAMP,
    fail_reason VARCHAR(255),
    cancel_requested_at TIMESTAMP,
    refunded_at TIMESTAMP,
    cancel_fail_reason VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id),
    product_id BIGINT REFERENCES product(id),
    quantity INTEGER NOT NULL,
    price INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

INSERT INTO users (id, email, password, name, role, created_at) VALUES
    (1, 'admin@limitedgoods.dev', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6eJJS8iU7tTF6wQYdQzb0xJgtqGO6', 'Demo Admin', 'ADMIN', NOW() - INTERVAL '30 days'),
    (2, 'buyer01@limitedgoods.dev', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6eJJS8iU7tTF6wQYdQzb0xJgtqGO6', 'Kim Buyer', 'USER', NOW() - INTERVAL '12 days'),
    (3, 'buyer02@limitedgoods.dev', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6eJJS8iU7tTF6wQYdQzb0xJgtqGO6', 'Lee Buyer', 'USER', NOW() - INTERVAL '5 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO product (id, name, description, price, stock) VALUES
    (1, 'Archive Logo Hoodie', 'Limited drop hoodie with heavyweight cotton fleece.', 89000, 24),
    (2, 'Weekend Denim Jacket', 'Small batch denim jacket for seasonal launch traffic demos.', 129000, 8),
    (3, 'Daily Ribbed T-Shirt', 'Core item used for normal product listing and order demos.', 32000, 120),
    (4, 'Signature Ball Cap', 'Low stock item for inventory warning scenarios.', 29000, 3),
    (5, 'Canvas Mini Tote', 'Sold-out item for sold-out display and admin restock demos.', 45000, 0),
    (6, 'Wool Blend Cardigan', 'Premium item used for high-price filter and order total demos.', 158000, 12),
    (7, 'Limited Ceramic Mug', 'Small goods item for low price checkout flow demos.', 18000, 42),
    (8, 'Drop No. 1 Sneakers', 'High-demand item for concurrent order and oversell prevention demos.', 149000, 10),
    (9, 'Gift Package Set', 'Bundle item for multiple quantity order demos.', 59000, 15),
    (10, 'Winter Muffler', 'Seasonal item for product search and admin update demos.', 39000, 6)
ON CONFLICT (id) DO NOTHING;

INSERT INTO orders (
    id,
    user_id,
    total_price,
    status,
    created_at,
    updated_at,
    paid_at,
    failed_at,
    expires_at,
    fail_reason,
    cancel_requested_at,
    refunded_at,
    cancel_fail_reason
) VALUES
    (1, 2, 89000, 'PAID', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NULL, NOW() - INTERVAL '3 days' + INTERVAL '10 minutes', NULL, NULL, NULL, NULL),
    (2, 2, 58000, 'PAYMENT_FAILED', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '10 minutes', 'Card authorization failed', NULL, NULL, NULL),
    (3, 3, 149000, 'CREATED', NOW() - INTERVAL '5 minutes', NOW() - INTERVAL '5 minutes', NULL, NULL, NOW() + INTERVAL '5 minutes', NULL, NULL, NULL, NULL),
    (4, 3, 45000, 'EXPIRED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day' + INTERVAL '10 minutes', NULL, NULL, NOW() - INTERVAL '1 day' + INTERVAL '10 minutes', NULL, NULL, NULL, NULL),
    (5, 2, 158000, 'CANCEL_REQUESTED', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '6 hours', NULL, NOW() - INTERVAL '6 hours' + INTERVAL '10 minutes', NULL, NOW() - INTERVAL '1 hour', NULL, NULL),
    (6, 3, 59000, 'REFUNDED', NOW() - INTERVAL '7 days', NOW() - INTERVAL '6 days', NOW() - INTERVAL '7 days', NULL, NOW() - INTERVAL '7 days' + INTERVAL '10 minutes', NULL, NOW() - INTERVAL '6 days' - INTERVAL '1 hour', NOW() - INTERVAL '6 days', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, product_id, quantity, price) VALUES
    (1, 1, 1, 1, 89000),
    (2, 2, 4, 2, 29000),
    (3, 3, 8, 1, 149000),
    (4, 4, 5, 1, 45000),
    (5, 5, 6, 1, 158000),
    (6, 6, 9, 1, 59000)
ON CONFLICT (id) DO NOTHING;

SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1), true);
SELECT setval('product_id_seq', COALESCE((SELECT MAX(id) FROM product), 1), true);
SELECT setval('orders_id_seq', COALESCE((SELECT MAX(id) FROM orders), 1), true);
SELECT setval('order_items_id_seq', COALESCE((SELECT MAX(id) FROM order_items), 1), true);
