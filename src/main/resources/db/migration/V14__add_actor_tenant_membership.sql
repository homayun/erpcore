-- =====================================================================
-- V14: sec_actor_tenant_membership
-- =====================================================================
-- Each actor may belong to one or more tenants. The "primary" tenant is
-- implicit (sec_credentials.tenant_id). The ACTIVE tenant is resolved per
-- session by TenantContextRestorationFilter, falling back to the primary.
-- =====================================================================

CREATE TABLE sec_actor_tenant_membership (
    actor_id  UUID NOT NULL,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (actor_id, tenant_id),
    CONSTRAINT fk_atm_actor FOREIGN KEY (actor_id)
        REFERENCES sec_credentials (actor_id) ON DELETE CASCADE
);

CREATE INDEX idx_atm_tenant ON sec_actor_tenant_membership (tenant_id);
CREATE INDEX idx_atm_actor  ON sec_actor_tenant_membership (actor_id);

-- Backfill: every existing credential implies membership in its tenant.
INSERT INTO sec_actor_tenant_membership (actor_id, tenant_id)
SELECT actor_id, tenant_id FROM sec_credentials
WHERE NOT EXISTS (
    SELECT 1 FROM sec_actor_tenant_membership m
    WHERE m.actor_id = sec_credentials.actor_id
      AND m.tenant_id = sec_credentials.tenant_id
);