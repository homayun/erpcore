package com.erp.core.admin.internal.company.adapter.out.jpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {
    List<BranchEntity> findByTenantId(UUID tenantId);
    boolean existsByCompanyId(UUID companyId);

    @Query(value = "SELECT b.* FROM admin_branch b " +
                   "INNER JOIN admin_company c ON c.id = b.company_id " +
                   "WHERE b.tenant_id = :tenantId AND c.master_company_id = :divisionId",
           nativeQuery = true)
    List<BranchEntity> findByTenantAndDivision(@Param("tenantId") UUID tenantId,
                                               @Param("divisionId") UUID divisionId);
}