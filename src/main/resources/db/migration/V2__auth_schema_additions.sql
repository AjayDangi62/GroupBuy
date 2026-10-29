-- GroupBuy Phase 2: Authentication support schema additions
-- Flyway Migration: V2__auth_schema_additions.sql
-- Adds family_id column to refresh_tokens for rotation reuse detection

ALTER TABLE refresh_tokens ADD COLUMN family_id VARCHAR(64) NOT NULL DEFAULT '';
ALTER TABLE refresh_tokens ADD COLUMN replaced_by_id BIGINT;
ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_replaced_by
    FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens(id);

-- Index for efficient family-based revocation queries
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
