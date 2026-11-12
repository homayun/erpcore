-- =====================================================================
-- V15: sec_tenant — first-class tenant registry
-- =====================================================================

CREATE TABLE sec_tenant (
    id                UUID              PRIMARY KEY,
    code              VARCHAR(50)       NOT NULL UNIQUE,
    name              VARCHAR(255)      NOT NULL,
    status            VARCHAR(20)       NOT NULL DEFAULT 'ACTIVE',
    subscription_plan VARCHAR(50),
    suspended_at      TIMESTAMP WITH TIME ZONE,
    suspended_by      UUID,
    suspended_reason  VARCHAR(500),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by        UUID,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tenant_status
        CHECK (status IN ('ACTIVE','SUSPENDED','ARCHIVED')),
    CONSTRAINT chk_tenant_code
        CHECK (code = UPPER(code))
);

CREATE INDEX idx_tenant_status ON sec_tenant (status);
CREATE INDEX idx_tenant_code   ON sec_tenant (code);

-- Bootstrap the "default" tenant used by single-tenant dev installs.
-- This ID is referenced by sec_tenant FK backfills in V17.
INSERT INTO sec_tenant (id, code, name, status, subscription_plan, created_at)
VALUES ('11111111-1111-1111-1111-111111111111',
        'DEFAULT', 'Default Tenant', 'ACTIVE', 'ENTERPRISE', CURRENT_TIMESTAMP);

-- Register the platform-level permission.
INSERT INTO sec_permission (permission_key, group_key, description, is_sensitive)
SELECT 'TENANT_MANAGE', 'PLATFORM', 'Create, edit, and disable tenants', true
WHERE NOT EXISTS (SELECT 1 FROM sec_permission WHERE permission_key = 'TENANT_MANAGE');