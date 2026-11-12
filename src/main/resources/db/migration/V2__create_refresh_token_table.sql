-- =====================================================================
-- V2: sec_refresh_token — opaque refresh tokens with family-based rotation
-- =====================================================================
-- Only token_hash is stored. Raw tokens never hit the database.
-- (family_id, expires_at, revoked_at) support reuse detection and cleanup.
-- =====================================================================

CREATE TABLE sec_refresh_token (
    id          UUID                       PRIMARY KEY,
    actor_id    UUID                       NOT NULL,
    tenant_id   UUID                       NOT NULL,
    token_hash  VARCHAR(64)                NOT NULL,
    family_id   UUID                       NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked_at  TIMESTAMP(6) WITH TIME ZONE,
    replaced_by UUID,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_actor
        FOREIGN KEY (actor_id) REFERENCES sec_credentials (actor_id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_replaced_by
        FOREIGN KEY (replaced_by) REFERENCES sec_refresh_token (id) ON DELETE SET NULL,
    CONSTRAINT chk_refresh_token_hash_len CHECK (CHAR_LENGTH(token_hash) = 64),
    CONSTRAINT chk_refresh_token_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_token_family     ON sec_refresh_token (family_id);
CREATE INDEX idx_refresh_token_expires_at ON sec_refresh_token (expires_at);
CREATE INDEX idx_refresh_token_revoked_at ON sec_refresh_token (revoked_at);
CREATE INDEX idx_refresh_token_actor      ON sec_refresh_token (actor_id);