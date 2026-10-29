-- GroupBuy Phase 1: Core Relational Database Schema
-- Flyway Migration: V1__initial_schema.sql
-- Strategy: All timestamps use TIMESTAMPTZ and are stored in UTC.
-- Monetary amounts use NUMERIC(12, 2) with explicit non-negative check constraints.

-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'SELLER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_users_role_status ON users(role, status);

-- 2. Products Table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    total_inventory INT NOT NULL,
    available_inventory INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT chk_products_base_price CHECK (base_price > 0),
    CONSTRAINT chk_products_inventory CHECK (total_inventory >= 0 AND available_inventory >= 0 AND available_inventory <= total_inventory),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'OUT_OF_STOCK'))
);

CREATE INDEX idx_products_seller_id ON products(seller_id);
CREATE INDEX idx_products_status ON products(status);

-- 3. Campaigns Table
CREATE TABLE campaigns (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_min_quantity INT NOT NULL,
    max_campaign_quantity INT,
    current_paid_units INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    finalized_at TIMESTAMPTZ,
    final_unit_price NUMERIC(12, 2),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_campaigns_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_campaigns_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT chk_campaigns_target_min CHECK (target_min_quantity > 0),
    CONSTRAINT chk_campaigns_max_quantity CHECK (max_campaign_quantity IS NULL OR max_campaign_quantity >= target_min_quantity),
    CONSTRAINT chk_campaigns_current_units CHECK (current_paid_units >= 0),
    CONSTRAINT chk_campaigns_time_window CHECK (ends_at > starts_at),
    CONSTRAINT chk_campaigns_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ENDED', 'FINALIZING', 'FINALIZED', 'CANCELLED'))
);

CREATE INDEX idx_campaigns_status_ends_at ON campaigns(status, ends_at);
CREATE INDEX idx_campaigns_product_id ON campaigns(product_id);
CREATE INDEX idx_campaigns_seller_id ON campaigns(seller_id);

-- 4. Pricing Tiers Table
CREATE TABLE pricing_tiers (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    min_quantity INT NOT NULL,
    max_quantity INT,
    unit_price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pricing_tiers_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT uq_pricing_tiers_campaign_min UNIQUE (campaign_id, min_quantity),
    CONSTRAINT chk_pricing_tiers_min_qty CHECK (min_quantity > 0),
    CONSTRAINT chk_pricing_tiers_range CHECK (max_quantity IS NULL OR max_quantity >= min_quantity),
    CONSTRAINT chk_pricing_tiers_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_pricing_tiers_campaign ON pricing_tiers(campaign_id, min_quantity);

-- 5. Participations Table
CREATE TABLE participations (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    authorized_unit_price NUMERIC(12, 2) NOT NULL,
    authorized_total_amount NUMERIC(12, 2) NOT NULL,
    final_unit_price NUMERIC(12, 2),
    final_total_amount NUMERIC(12, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_participations_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_participations_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_participations_quantity CHECK (quantity > 0),
    CONSTRAINT chk_participations_auth_unit_price CHECK (authorized_unit_price >= 0),
    CONSTRAINT chk_participations_auth_total CHECK (authorized_total_amount >= 0),
    CONSTRAINT chk_participations_status CHECK (status IN ('INITIATED', 'AUTHORIZED', 'CONFIRMED', 'CANCELLED', 'REFUNDED'))
);

CREATE INDEX idx_participations_campaign_status ON participations(campaign_id, status);
CREATE INDEX idx_participations_user_id ON participations(user_id);

-- 6. Inventory Reservations Table
CREATE TABLE inventory_reservations (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    participation_id BIGINT,
    reserved_quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inv_res_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_inv_res_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_inv_res_participation FOREIGN KEY (participation_id) REFERENCES participations(id),
    CONSTRAINT chk_inv_res_quantity CHECK (reserved_quantity > 0),
    CONSTRAINT chk_inv_res_status CHECK (status IN ('PENDING', 'COMMITTED', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX idx_inv_res_product_status ON inventory_reservations(product_id, status);
CREATE INDEX idx_inv_res_expires_at ON inventory_reservations(status, expires_at);

-- 7. Payments Table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    participation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    authorized_amount NUMERIC(12, 2) NOT NULL,
    captured_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    refunded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_gateway VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payments_participation FOREIGN KEY (participation_id) REFERENCES participations(id),
    CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_payments_authorized CHECK (authorized_amount >= 0),
    CONSTRAINT chk_payments_captured CHECK (captured_amount >= 0 AND captured_amount <= authorized_amount),
    CONSTRAINT chk_payments_refunded CHECK (refunded_amount >= 0 AND refunded_amount <= captured_amount),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'PARTIALLY_REFUNDED', 'REFUNDED', 'FAILED'))
);

CREATE INDEX idx_payments_participation ON payments(participation_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);

-- 8. Payment Attempts Table
CREATE TABLE payment_attempts (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    gateway_transaction_id VARCHAR(255),
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    error_code VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_attempts_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT chk_payment_attempts_num CHECK (attempt_number > 0),
    CONSTRAINT chk_payment_attempts_amount CHECK (amount >= 0),
    CONSTRAINT chk_payment_attempts_status CHECK (status IN ('INITIATED', 'SUCCESS', 'FAILURE', 'TIMEOUT'))
);

CREATE INDEX idx_payment_attempts_payment ON payment_attempts(payment_id, attempt_number);
CREATE INDEX idx_payment_attempts_gateway_tx ON payment_attempts(gateway_transaction_id);

-- 9. Payment Events Table
CREATE TABLE payment_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_events_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

CREATE INDEX idx_payment_events_payment_type ON payment_events(payment_id, event_type);

-- 10. Orders Table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL,
    participation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    final_unit_price NUMERIC(12, 2) NOT NULL,
    total_price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    shipping_address TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_participation FOREIGN KEY (participation_id) REFERENCES participations(id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_orders_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_orders_quantity CHECK (quantity > 0),
    CONSTRAINT chk_orders_prices CHECK (final_unit_price >= 0 AND total_price >= 0),
    CONSTRAINT chk_orders_status CHECK (status IN ('CREATED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_campaign_id ON orders(campaign_id);
CREATE INDEX idx_orders_status ON orders(status);

-- 11. Refunds Table
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    refund_reference VARCHAR(100) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refunds_reference UNIQUE (refund_reference),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT chk_refunds_amount CHECK (amount > 0),
    CONSTRAINT chk_refunds_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_refunds_payment ON refunds(payment_id, status);

-- 12. Audit Logs Table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100),
    ip_address VARCHAR(45),
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- 13. Idempotency Records Table
CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INT,
    response_body TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PROCESSING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_idempotency_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_idempotency_key_expires ON idempotency_records(idempotency_key, expires_at);

-- 14. Refresh Tokens Table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_expires ON refresh_tokens(user_id, expires_at);
