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
    public List<Company> getCompanies(UUID tenantId, String filter) {
        List<CompanyEntity> list = companyRepo.findByTenantId(tenantId);
        if ("active".equals(filter)) return list.stream().filter(CompanyEntity::isActive).map(CompanyEntity::toDomain).toList();
        if ("deleted".equals(filter)) return list.stream().filter(e -> !e.isActive()).map(CompanyEntity::toDomain).toList();
        return list.stream().map(CompanyEntity::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public List<MasterCompany> getMasterCompanies(UUID tenantId) {
        return masterRepo.findByTenantId(tenantId).stream().map(MasterCompanyEntity::toDomain).toList();
    }

    @Transactional
    public Company createCompany(UUID tenantId, UUID masterCompanyId, String name, String regNumber, String taxId) {
        Company c = new Company(UUID.randomUUID(), tenantId, masterCompanyId, name, regNumber, taxId, null, null, null, true, Instant.now());
        companyRepo.save(CompanyEntity.fromDomain(c));
        return c;
    }

    @Transactional(readOnly = true)
    public Company getById(UUID id) {
        return companyRepo.findById(id).map(CompanyEntity::toDomain).orElseThrow(() -> new IllegalArgumentException("Not found"));
    }

    @Transactional
    public Company updateCompany(UUID id, UUID masterCompanyId, String name, String regNumber, String taxId) {
        CompanyEntity e = companyRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        e.update(masterCompanyId, name, regNumber, taxId);
        companyRepo.save(e);
        return e.toDomain();
    }

    @Transactional
    public void softDeleteCompany(UUID id) {
        if (branchRepo.existsByCompanyId(id)) {
            throw new IllegalStateException("Cannot delete! This company has active Branches.");
        }
        CompanyEntity e = companyRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        e.setActive(false);
        companyRepo.save(e);
    }

    @Transactional
    public void restoreCompany(UUID id) {
        CompanyEntity e = companyRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        e.setActive(true);
        companyRepo.save(e);
    }

    @Transactional
    public void hardDeleteCompany(UUID id) {
        if (branchRepo.existsByCompanyId(id)) {
            throw new IllegalStateException("Cannot permanently delete! This company has Branches.");
        }
        companyRepo.deleteById(id);
    }
}