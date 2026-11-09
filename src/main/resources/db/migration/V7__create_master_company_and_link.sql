CREATE TABLE master_companies (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert a default Master Company (Group)
INSERT INTO master_companies (id, tenant_id, name, code, is_active, created_at)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Default Group', 'GRP-001', true, CURRENT_TIMESTAMP);

-- Add master_company_id to admin_company table (Corrected table name)
ALTER TABLE admin_company ADD COLUMN master_company_id UUID;
ALTER TABLE admin_company ADD CONSTRAINT fk_admin_company_master_company FOREIGN KEY (master_company_id) REFERENCES master_companies(id);

-- Set existing companies to the default group
UPDATE admin_company SET master_company_id = '22222222-2222-2222-2222-222222222222';
ALTER TABLE admin_company ALTER COLUMN master_company_id SET NOT NULL;