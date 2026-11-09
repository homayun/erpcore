-- Ensure sec_user_permission references sec_permission (cascade on delete)
ALTER TABLE sec_user_permission
    ADD CONSTRAINT fk_user_permission_permission
    FOREIGN KEY (permission_key) REFERENCES sec_permission (permission_key) ON DELETE CASCADE;