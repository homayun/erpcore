-- =====================================================================
-- V3: RBAC + ABAC-light catalog
-- =====================================================================
-- Permissions are DECLARED by business modules at startup and persisted here.
-- Roles and role-permission grants are managed by the Tenant/Admin module.
-- Attribute conditions add lightweight ABAC per permission.
-- is_active on sec_permission was added in V10; moved here for a clean
-- fresh-install schema. V10 is now data-only.
-- =====================================================================

CREATE TABLE sec_permission (
    permission_key VARCHAR(100)  PRIMARY KEY,
    group_key      VARCHAR(50)   NOT NULL,
    description    VARCHAR(500)  NOT NULL,
    is_sensitive   BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_permission_key_format
        CHECK (permission_key ~ '^[A-Z][A-Z0-9_]{2,99}$')
);

CREATE TABLE sec_role (
    id              UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    name            VARCHAR(100) NOT NULL,
    hierarchy_level INT          NOT NULL DEFAULT 1,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_role_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_role_hierarchy CHECK (hierarchy_level BETWEEN 1 AND 1000)
);

CREATE TABLE sec_role_permission (
    role_id        UUID         NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission_key),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id)
        REFERENCES sec_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_key)
        REFERENCES sec_permission (permission_key) ON DELETE CASCADE
);

CREATE TABLE sec_role_assignment (
    id        UUID PRIMARY KEY,
    actor_id  UUID NOT NULL,
    role_id   UUID NOT NULL,
    tenant_id UUID NOT NULL,
    CONSTRAINT fk_ra_role FOREIGN KEY (role_id)
        REFERENCES sec_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_ra_actor FOREIGN KEY (actor_id)
        REFERENCES sec_credentials (actor_id) ON DELETE CASCADE,
    CONSTRAINT uk_ra_actor_role UNIQUE (actor_id, role_id)
);

CREATE INDEX idx_ra_actor_tenant ON sec_role_assignment (actor_id, tenant_id);
CREATE INDEX idx_ra_role         ON sec_role_assignment (role_id);

CREATE TABLE sec_attribute_condition (
    id              UUID         PRIMARY KEY,
    permission_key  VARCHAR(100) NOT NULL,
    attribute_name  VARCHAR(100) NOT NULL,
    operator        VARCHAR(30)  NOT NULL,
    condition_value VARCHAR(500) NOT NULL,
    CONSTRAINT fk_ac_permission FOREIGN KEY (permission_key)
        REFERENCES sec_permission (permission_key) ON DELETE CASCADE,
    CONSTRAINT chk_ac_operator
        CHECK (operator IN ('EQUALS','IN','LTE','GTE','CONTAINS_ANY'))
);

CREATE INDEX idx_ac_permission ON sec_attribute_condition (permission_key);