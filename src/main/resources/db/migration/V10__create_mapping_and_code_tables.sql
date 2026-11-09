-- Add is_active to permissions
ALTER TABLE sec_permission ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create code setting table
CREATE TABLE admin_code_setting (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    category VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Direct user permissions (optional mapping)
CREATE TABLE sec_user_permission (
    actor_id UUID NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    tenant_id UUID NOT NULL,
    PRIMARY KEY (actor_id, permission_key)
);