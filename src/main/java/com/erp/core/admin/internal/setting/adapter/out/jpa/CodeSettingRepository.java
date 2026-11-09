package com.erp.core.admin.internal.setting.adapter.out.jpa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface CodeSettingRepository extends JpaRepository<CodeSettingEntity, UUID> {
    List<CodeSettingEntity> findByTenantId(UUID tenantId);
}