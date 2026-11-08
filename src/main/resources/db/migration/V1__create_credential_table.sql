-- =====================================================================
-- V1: sec_credentials â€” authentication credential store
-- =====================================================================
-- Uniqueness: (tenant_id, username). Two tenants MAY share a username.
-- Types chosen to be compatible with both H2 2.x and PostgreSQL 16+.

CREATE TABLE sec_credentials (
    actor_id      UUID            PRIMARY KEY,
    tenant_id     UUID            NOT NULL,
    username      VARCHAR(255)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    CONSTRAINT uk_credentials_tenant_username UNIQUE (tenant_id, username)
);

-- Fast lookup by (tenant, username) â€” used on every login attempt.
CREATE INDEX idx_credentials_tenant_username
    ON sec_credentials (tenant_id, username);