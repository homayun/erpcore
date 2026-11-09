package com.erp.core.admin.internal.company.application;
import com.erp.core.admin.internal.company.adapter.out.jpa.*;
import com.erp.core.admin.internal.company.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CompanyService {
    private final CompanyRepository companyRepo;
    private final BranchRepository branchRepo;
    public CompanyService(CompanyRepository c, BranchRepository b) { this.companyRepo = c; this.branchRepo = b; }

    @Transactional(readOnly = true)
    public List<Company> getCompanies(UUID tenantId) { return companyRepo.findByTenantId(tenantId).stream().map(CompanyEntity::toDomain).toList(); }
    
    @Transactional
    public void createCompany(UUID tenantId, String name, String regNumber, String taxId) {
        Company c = new Company(UUID.randomUUID(), tenantId, name, regNumber, taxId, null, null, null, true, Instant.now());
        companyRepo.save(CompanyEntity.fromDomain(c));
    }

    @Transactional(readOnly = true)
    public List<Branch> getBranches(UUID tenantId) { return branchRepo.findByTenantId(tenantId).stream().map(BranchEntity::toDomain).toList(); }
}