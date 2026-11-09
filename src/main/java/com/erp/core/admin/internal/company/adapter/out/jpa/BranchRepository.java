package com.erp.core.admin.internal.company.adapter.out.jpa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {
    List<BranchEntity> findByTenantId(UUID tenantId);
    boolean existsByCompanyId(UUID companyId);
}