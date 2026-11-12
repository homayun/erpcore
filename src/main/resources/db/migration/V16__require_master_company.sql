-- =====================================================================
-- V16: require master_company_id on admin_company
-- =====================================================================
-- Model B (Holding + Divisions): every company must belong to a division.
-- =====================================================================

UPDATE admin_company ac
SET master_company_id = (
    SELECT mc.id FROM master_companies mc
    WHERE mc.tenant_id = ac.tenant_id
    ORDER BY mc.created_at
    LIMIT 1
)
WHERE ac.master_company_id IS NULL
  AND EXISTS (SELECT 1 FROM master_companies mc WHERE mc.tenant_id = ac.tenant_id);

ALTER TABLE admin_company ALTER COLUMN master_company_id SET NOT NULL;