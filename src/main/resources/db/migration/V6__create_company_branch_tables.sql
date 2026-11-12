CREATE TABLE admin_company (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    name                VARCHAR(255) NOT NULL,
    registration_number VARCHAR(100),
    tax_id              VARCHAR(100),
    email               VARCHAR(150),
    phone               VARCHAR(50),
    address             TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_company_tenant ON admin_company (tenant_id, is_active);
CREATE INDEX idx_company_regno  ON admin_company (registration_number);

CREATE TABLE admin_branch (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    company_id  UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    branch_code VARCHAR(50) NOT NULL,
    address     TEXT,
    phone       VARCHAR(50),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- RESTRICT: hard-deleting a company MUST NOT silently destroy branches.
    -- App-level CompanyService.hardDeleteCompany already checks; this is defense-in-depth.
    CONSTRAINT fk_branch_company FOREIGN KEY (company_id)
        REFERENCES admin_company(id) ON DELETE RESTRICT,

    CONSTRAINT uk_branch_tenant_code UNIQUE (tenant_id, branch_code)
);

CREATE INDEX idx_branch_tenant  ON admin_branch (tenant_id, is_active);
CREATE INDEX idx_branch_company ON admin_branch (company_id);