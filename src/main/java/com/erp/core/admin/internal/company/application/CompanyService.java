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
    private final MasterCompanyRepository masterRepo;

    public CompanyService(CompanyRepository c, BranchRepository b, MasterCompanyRepository m) { 
        this.companyRepo = c; 
        this.branchRepo = b;
        this.masterRepo = m;
    }

    @Transactional(readOnly = true)
    public List<Company> getCompanies(UUID tenantId) { 
        return companyRepo.findByTenantId(tenantId).stream().map(CompanyEntity::toDomain).toList(); 
    }

    @Transactional(readOnly = true)
    public List<MasterCompany> getMasterCompanies(UUID tenantId) {
        return masterRepo.findByTenantId(tenantId).stream().map(MasterCompanyEntity::toDomain).toList();
    }

    @Transactional
    public MasterCompany createMasterCompany(UUID tenantId, String name, String code) {
        MasterCompany mc = new MasterCompany(UUID.randomUUID(), tenantId, name, code, true, Instant.now());
        masterRepo.save(MasterCompanyEntity.fromDomain(mc));
        return mc;
    }

    @Transactional
    public void deleteMasterCompany(UUID id) {
        masterRepo.deleteById(id);
    }

    @Transactional
    public Company createCompany(UUID tenantId, UUID masterCompanyId, String name, String regNumber, String taxId) {
        Company c = new Company(UUID.randomUUID(), tenantId, masterCompanyId, name, regNumber, taxId, null, null, null, true, Instant.now());
        companyRepo.save(CompanyEntity.fromDomain(c));
        return c;
    }

    @Transactional
    public void deleteCompany(UUID id) {
        companyRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Branch> getBranches(UUID tenantId) { 
        return branchRepo.findByTenantId(tenantId).stream().map(BranchEntity::toDomain).toList(); 
    }
}