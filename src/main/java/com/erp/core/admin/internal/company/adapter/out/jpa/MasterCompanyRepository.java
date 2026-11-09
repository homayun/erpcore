package com.erp.core.admin.internal.company.adapter.out.jpa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface MasterCompanyRepository extends JpaRepository<MasterCompanyEntity, UUID> {
    List<MasterCompanyEntity> findByTenantId(UUID tenantId);
}