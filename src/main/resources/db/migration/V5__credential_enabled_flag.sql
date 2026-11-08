-- =====================================================================
-- V5: enabled flag on credentials (disable an account without deleting it)
-- =====================================================================
ALTER TABLE sec_credentials ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;