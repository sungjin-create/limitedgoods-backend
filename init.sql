-- Limited Goods PostgreSQL bootstrap schema.
-- docker-entrypoint-initdb.d executes this file only when the database volume is empty.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- -----------------------------------------------------------------------------
-- Users and products
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price INTEGER NOT NULL,
    initial_stock INTEGER NOT NULL,
    stock INTEGER NOT NULL,
    sold_count INTEGER NOT NULL DEFAULT 0,
    max_purchase_quantity INTEGER,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    sale_start_at TIMESTAMP WITHOUT TIME ZONE,
    sale_end_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT ck_product_price CHECK (price >= 0),
    CONSTRAINT ck_product_initial_stock CHECK (initial_stock >= 0),
    CONSTRAINT ck_product_stock CHECK (stock >= 0),
    CONSTRAINT ck_product_sold_count CHECK (sold_count >= 0),
    CONSTRAINT ck_product_max_purchase_quantity
        CHECK (max_purchase_quantity IS NULL OR max_purchase_quantity > 0),
    CONSTRAINT ck_product_type CHECK (type IN ('NORMAL', 'LIMITED')),
    CONSTRAINT ck_product_status CHECK (
        status IN ('DRAFT', 'PREPARING', 'SCHEDULED', 'ACTIVE', 'PAUSED', 'HIDDEN', 'ARCHIVED')
    ),
    CONSTRAINT ck_product_sale_period CHECK (
        sale_start_at IS NULL OR sale_end_at IS NULL OR sale_end_at > sale_start_at
    )
);

-- -----------------------------------------------------------------------------
-- Cart
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS cart (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_cart_user UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS cart_item (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    price INTEGER NOT NULL,
    total_price BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT fk_cart_item_cart
        FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE RESTRICT,
    CONSTRAINT uq_cart_item_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT ck_cart_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_cart_item_price CHECK (price >= 0),
    CONSTRAINT ck_cart_item_total_price CHECK (total_price >= 0)
);

-- -----------------------------------------------------------------------------
-- Orders and payments
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_price BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    paid_at TIMESTAMP WITHOUT TIME ZONE,
    failed_at TIMESTAMP WITHOUT TIME ZONE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    fail_reason VARCHAR(255),
    cancel_requested_at TIMESTAMP WITHOUT TIME ZONE,
    refunded_at TIMESTAMP WITHOUT TIME ZONE,
    cancel_fail_reason VARCHAR(255),
    checkout_token VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_orders_user_checkout_token UNIQUE (user_id, checkout_token),
    CONSTRAINT ck_orders_total_price CHECK (total_price >= 0),
    CONSTRAINT ck_orders_status CHECK (
        status IN (
            'CREATED',
            'PAYMENT_PENDING',
            'PAYMENT_APPROVED',
            'PAID',
            'PAYMENT_FAILED',
            'CANCEL_REQUESTED',
            'CANCEL_FAILED',
            'REFUNDED',
            'CANCELED',
            'COMPLETED',
            'EXPIRED'
        )
    ),
    CONSTRAINT ck_orders_request_fingerprint_length
        CHECK (char_length(request_fingerprint) = 64)
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    price INTEGER NOT NULL,
    line_total_price BIGINT NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE RESTRICT,
    CONSTRAINT uq_order_items_order_product UNIQUE (order_id, product_id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_price CHECK (price >= 0),
    CONSTRAINT ck_order_items_line_total_price CHECK (line_total_price >= 0)
);

CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_history_user
        FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_order_status_history_from_status CHECK (
        from_status IS NULL OR from_status IN (
            'CREATED',
            'PAYMENT_PENDING',
            'PAYMENT_APPROVED',
            'PAID',
            'PAYMENT_FAILED',
            'CANCEL_REQUESTED',
            'CANCEL_FAILED',
            'REFUNDED',
            'CANCELED',
            'COMPLETED',
            'EXPIRED'
        )
    ),
    CONSTRAINT ck_order_status_history_to_status CHECK (
        to_status IN (
            'CREATED',
            'PAYMENT_PENDING',
            'PAYMENT_APPROVED',
            'PAID',
            'PAYMENT_FAILED',
            'CANCEL_REQUESTED',
            'CANCEL_FAILED',
            'REFUNDED',
            'CANCELED',
            'COMPLETED',
            'EXPIRED'
        )
    )
);

CREATE TABLE IF NOT EXISTS payment_attempt (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    pg_transaction_id VARCHAR(255),
    failure_code VARCHAR(255),
    failure_reason VARCHAR(255),
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    approved_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT fk_payment_attempt_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT uq_payment_attempt_order_key UNIQUE (order_id, idempotency_key),
    CONSTRAINT uq_payment_attempt_pg_transaction UNIQUE (pg_transaction_id),
    CONSTRAINT ck_payment_attempt_amount CHECK (amount >= 0),
    CONSTRAINT ck_payment_attempt_status
        CHECK (status IN ('PROCESSING', 'APPROVED', 'DECLINED', 'UNKNOWN')),
    CONSTRAINT ck_payment_attempt_fingerprint_length
        CHECK (char_length(request_fingerprint) = 64),
    CONSTRAINT ck_payment_attempt_idempotency_key
        CHECK (char_length(idempotency_key) BETWEEN 8 AND 100)
);

-- -----------------------------------------------------------------------------
-- Transactional outbox and internal consumers
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    last_tried_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    published_at TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT ck_outbox_event_type
        CHECK (event_type IN ('ORDER_PAID', 'ORDER_EXPIRED', 'ORDER_CANCELED')),
    CONSTRAINT ck_outbox_event_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_event_retry_count CHECK (retry_count >= 0)
);

CREATE TABLE IF NOT EXISTS internal_processed_event (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    consumer_name VARCHAR(255) NOT NULL,

    CONSTRAINT uq_internal_processed_event UNIQUE (event_id, consumer_name)
);

CREATE TABLE IF NOT EXISTS internal_email_delivery (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    template VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITHOUT TIME ZONE,
    last_error TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT fk_internal_email_delivery_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT uq_internal_email_delivery_event UNIQUE (event_id),
    CONSTRAINT ck_internal_email_delivery_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_internal_email_delivery_retry_count CHECK (retry_count >= 0)
);

-- -----------------------------------------------------------------------------
-- Read projections
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS product_sales_projection (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    order_count BIGINT NOT NULL DEFAULT 0,
    sold_quantity BIGINT NOT NULL DEFAULT 0,
    revenue BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_product_sales_projection_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT uq_product_sales_projection_product UNIQUE (product_id),
    CONSTRAINT ck_product_sales_projection_order_count CHECK (order_count >= 0),
    CONSTRAINT ck_product_sales_projection_sold_quantity CHECK (sold_quantity >= 0),
    CONSTRAINT ck_product_sales_projection_revenue CHECK (revenue >= 0)
);

-- -----------------------------------------------------------------------------
-- Query and worker indexes
-- -----------------------------------------------------------------------------

-- Product storefront, scheduling, stock monitoring and contains-search.
CREATE INDEX IF NOT EXISTS idx_product_status_id
    ON product (status, id);

CREATE INDEX IF NOT EXISTS idx_product_scheduled_start
    ON product (sale_start_at, id)
    WHERE status = 'SCHEDULED';

CREATE INDEX IF NOT EXISTS idx_product_stock
    ON product (stock, id);

CREATE INDEX IF NOT EXISTS idx_product_name_trgm
    ON product USING GIN (upper(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_product_description_trgm
    ON product USING GIN (upper(description) gin_trgm_ops);

-- Product-only existence checks and cart cleanup by user/product.
CREATE INDEX IF NOT EXISTS idx_cart_item_product
    ON cart_item (product_id);

-- User order history and active-order locking.
CREATE INDEX IF NOT EXISTS idx_orders_user_created
    ON orders (user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_orders_user_status_id
    ON orders (user_id, status, id);

-- Expiration scheduler only scans states that can expire.
CREATE INDEX IF NOT EXISTS idx_orders_expirable
    ON orders (expires_at, id)
    WHERE status IN ('CREATED', 'PAYMENT_FAILED');

-- Dashboard and revenue range queries.
CREATE INDEX IF NOT EXISTS idx_orders_created_status
    ON orders (created_at, status);

CREATE INDEX IF NOT EXISTS idx_orders_paid_status
    ON orders (paid_at, status)
    WHERE paid_at IS NOT NULL;

-- The order/product unique constraint already supports order_id lookups.
CREATE INDEX IF NOT EXISTS idx_order_items_product_order
    ON order_items (product_id, order_id);

CREATE INDEX IF NOT EXISTS idx_order_status_history_order_created
    ON order_status_history (order_id, created_at, id);

-- Reconciliation only processes unresolved payment attempts.
CREATE INDEX IF NOT EXISTS idx_payment_attempt_reconciliation
    ON payment_attempt (updated_at, id)
    WHERE status IN ('PROCESSING', 'UNKNOWN');

-- Outbox and email workers fetch bounded batches in creation order.
CREATE INDEX IF NOT EXISTS idx_outbox_event_worker
    ON outbox_event (status, created_at, id)
    INCLUDE (retry_count);

CREATE INDEX IF NOT EXISTS idx_internal_email_delivery_worker
    ON internal_email_delivery (status, next_attempt_at, created_at, id);
