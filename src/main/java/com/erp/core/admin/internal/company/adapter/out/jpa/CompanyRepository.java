package com.erp.core.admin.internal.company.adapter.out.jpa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID> {
    List<CompanyEntity> findByTenantId(UUID tenantId);
}