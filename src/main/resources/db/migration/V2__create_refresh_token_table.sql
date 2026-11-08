-- =====================================================================
-- V2: sec_refresh_token â€” opaque refresh tokens with family-based rotation
-- =====================================================================
-- Only token_hash is stored. Raw tokens never hit the database.
-- (family_id, expires_at, revoked_at) support reuse detection and cleanup.

CREATE TABLE sec_refresh_token (
    id          UUID                    PRIMARY KEY,
    actor_id    UUID                    NOT NULL,
    tenant_id   UUID                    NOT NULL,
    token_hash  VARCHAR(64)             NOT NULL,
    family_id   UUID                    NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE  NOT NULL,
    expires_at  TIMESTAMP(6) WITH TIME ZONE  NOT NULL,
    revoked_at  TIMESTAMP(6) WITH TIME ZONE,
    replaced_by UUID,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

-- Family lookup for reuse detection (revoke entire family on replay).
CREATE INDEX idx_refresh_token_family
    ON sec_refresh_token (family_id);

-- Expiry cleanup â€” used by RefreshTokenCleanupJob.
CREATE INDEX idx_refresh_token_expires_at
    ON sec_refresh_token (expires_at);

-- Revocation cleanup â€” used by RefreshTokenCleanupJob for stale revoked tokens.
CREATE INDEX idx_refresh_token_revoked_at
    ON sec_refresh_token (revoked_at);