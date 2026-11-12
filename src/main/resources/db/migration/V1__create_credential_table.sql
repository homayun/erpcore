-- =====================================================================
-- V1: sec_credentials â€” authentication credential store
-- =====================================================================
-- Uniqueness: (tenant_id, username). Two tenants MAY share a username.
-- Types chosen to be compatible with H2 2.x and PostgreSQL 16+.
-- enabled column moves here (was in V5); V5 is now idempotent no-op.
-- =====================================================================

CREATE TABLE sec_credentials (
    actor_id            UUID            PRIMARY KEY,
    tenant_id           UUID            NOT NULL,
    username            VARCHAR(255)    NOT NULL,
    password_hash       VARCHAR(255)    NOT NULL,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    last_login_at       TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_credentials_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT chk_credentials_username_len CHECK (CHAR_LENGTH(TRIM(username)) BETWEEN 1 AND 255)
);

-- Fast lookup by (tenant, username) â€” used on every login attempt.
CREATE INDEX idx_credentials_tenant_username
    ON sec_credentials (tenant_id, username);

-- Admin pagination (findByTenant, countByTenant).
CREATE INDEX idx_credentials_tenant ON sec_credentials (tenant_id);

