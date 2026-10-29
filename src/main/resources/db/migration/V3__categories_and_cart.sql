-- GroupBuy Phase 3: Categories, Shopping Cart, Order Items & Campaign Enhancements
-- Flyway Migration: V3__categories_and_cart.sql

-- 1. Categories Table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description TEXT,
    parent_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_active ON categories(active);

-- 2. Add category_id and image_url to products table
ALTER TABLE products ADD COLUMN category_id BIGINT;
ALTER TABLE products ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id);
CREATE INDEX idx_products_category_id ON products(category_id);
ALTER TABLE products ADD COLUMN image_url VARCHAR(512);
CREATE INDEX idx_products_name ON products(name);

-- 3. Shopping Cart Table
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 4. Cart Items Table
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    campaign_id BIGINT,
    quantity INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_cart_items_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT uq_cart_items_cart_product_campaign UNIQUE (cart_id, product_id, campaign_id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product ON cart_items(product_id);

-- 5. Campaign Enhancements: min_participants, current_participants and flexible status
ALTER TABLE campaigns ADD COLUMN min_participants INT NOT NULL DEFAULT 1;
ALTER TABLE campaigns ADD COLUMN current_participants INT NOT NULL DEFAULT 0;
ALTER TABLE campaigns DROP CONSTRAINT chk_campaigns_status;
ALTER TABLE campaigns ADD CONSTRAINT chk_campaigns_status CHECK (
    status IN ('DRAFT', 'SCHEDULED', 'ACTIVE', 'ENDED', 'FINALIZING', 'FINALIZED', 'SUCCESSFUL', 'FAILED', 'EXPIRED', 'CANCELLED')
);

-- 6. Orders & Order Items Enhancements
ALTER TABLE orders ALTER COLUMN participation_id DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN campaign_id DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN product_id DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN quantity DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN final_unit_price DROP NOT NULL;

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    campaign_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INT NOT NULL,
    total_price NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_items_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (unit_price >= 0 AND total_price >= 0)
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- 7. Payments Enhancements
ALTER TABLE payments ALTER COLUMN participation_id DROP NOT NULL;
ALTER TABLE payments ADD COLUMN order_id BIGINT;
ALTER TABLE payments ADD CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE payments ADD COLUMN transaction_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(255);
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key);
