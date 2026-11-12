-- Set existing companies referencing the default group to NULL
UPDATE admin_company SET master_company_id = NULL WHERE master_company_id = '22222222-2222-2222-2222-222222222222';

-- Drop the NOT NULL constraint so companies can exist without a master group temporarily
ALTER TABLE admin_company ALTER COLUMN master_company_id DROP NOT NULL;

-- Delete the hardcoded Default Group
DELETE FROM master_companies WHERE id = '22222222-2222-2222-2222-222222222222';