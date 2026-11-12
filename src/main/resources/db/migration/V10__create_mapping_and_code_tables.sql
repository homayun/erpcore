-- =====================================================================
-- V10: admin_code_setting + sec_user_permission
-- is_active on sec_permission moved to V3 (fresh install).
-- =====================================================================

CREATE TABLE admin_code_setting (
    id         UUID PRIMARY KEY,
    tenant_id  UUID NOT NULL,
    category   VARCHAR(100) NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_code_setting_tenant_cat_code UNIQUE (tenant_id, category, code)
);

CREATE INDEX idx_code_setting_tenant_cat ON admin_code_setting (tenant_id, category, is_active);

CREATE TABLE sec_user_permission (
    actor_id       UUID NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    tenant_id      UUID NOT NULL,
    PRIMARY KEY (actor_id, permission_key),
    CONSTRAINT fk_user_permission_actor FOREIGN KEY (actor_id)
        REFERENCES sec_credentials (actor_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_permission_permission FOREIGN KEY (permission_key)
        REFERENCES sec_permission (permission_key) ON DELETE CASCADE
);

CREATE INDEX idx_user_permission_tenant ON sec_user_permission (tenant_id);