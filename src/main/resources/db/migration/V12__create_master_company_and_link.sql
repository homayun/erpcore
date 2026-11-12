CREATE TABLE master_companies (
    id         UUID PRIMARY KEY,
    tenant_id  UUID NOT NULL,
    name       VARCHAR(255) NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_master_company_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_master_company_tenant ON master_companies (tenant_id, is_active);

-- Link admin_company → master_companies (nullable initially; enforced NOT NULL in V16).
ALTER TABLE admin_company ADD COLUMN master_company_id UUID;
ALTER TABLE admin_company
    ADD CONSTRAINT fk_admin_company_master_company
    FOREIGN KEY (master_company_id) REFERENCES master_companies(id) ON DELETE RESTRICT;

CREATE INDEX idx_company_master ON admin_company (master_company_id);