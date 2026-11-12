-- Insert default permissions if they don't exist
INSERT INTO sec_permission (permission_key, group_key, description, is_sensitive) 
SELECT 'VIEW_DASHBOARD', 'DASHBOARD', 'Can view dashboard', false
WHERE NOT EXISTS (SELECT 1 FROM sec_permission WHERE permission_key = 'VIEW_DASHBOARD');

INSERT INTO sec_permission (permission_key, group_key, description, is_sensitive) 
SELECT 'MANAGE_USERS', 'ADMIN', 'Can manage users', true
WHERE NOT EXISTS (SELECT 1 FROM sec_permission WHERE permission_key = 'MANAGE_USERS');

INSERT INTO sec_permission (permission_key, group_key, description, is_sensitive) 
SELECT 'MANAGE_ROLES', 'ADMIN', 'Can manage roles', true
WHERE NOT EXISTS (SELECT 1 FROM sec_permission WHERE permission_key = 'MANAGE_ROLES');

INSERT INTO sec_permission (permission_key, group_key, description, is_sensitive) 
SELECT 'MANAGE_COMPANY', 'ADMIN', 'Can manage company settings', true
WHERE NOT EXISTS (SELECT 1 FROM sec_permission WHERE permission_key = 'MANAGE_COMPANY');