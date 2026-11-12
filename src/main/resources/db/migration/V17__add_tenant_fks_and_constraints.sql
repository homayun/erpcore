-- =====================================================================
-- V17: Tenant FKs ONLY
-- =====================================================================
-- Actor FKs are already defined in earlier migrations (V2, V3, V10, V14).
-- Do NOT re-add them here. This migration adds only tenant FKs that were
-- deferred because sec_tenant is created in V15.

ALTER TABLE sec_credentials
    ADD CONSTRAINT fk_credentials_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE RESTRICT;

ALTER TABLE sec_role
    ADD CONSTRAINT fk_role_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE CASCADE;

ALTER TABLE sec_role_assignment
    ADD CONSTRAINT fk_ra_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE CASCADE;

ALTER TABLE sec_user_permission
    ADD CONSTRAINT fk_user_permission_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE CASCADE;

ALTER TABLE sec_refresh_token
    ADD CONSTRAINT fk_refresh_token_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE CASCADE;

ALTER TABLE sec_actor_tenant_membership
    ADD CONSTRAINT fk_atm_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE CASCADE;

ALTER TABLE admin_company
    ADD CONSTRAINT fk_admin_company_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE RESTRICT;

ALTER TABLE admin_branch
    ADD CONSTRAINT fk_admin_branch_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE RESTRICT;

ALTER TABLE master_companies
    ADD CONSTRAINT fk_master_company_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE RESTRICT;

ALTER TABLE admin_code_setting
    ADD CONSTRAINT fk_code_setting_tenant
    FOREIGN KEY (tenant_id) REFERENCES sec_tenant (id) ON DELETE RESTRICT;