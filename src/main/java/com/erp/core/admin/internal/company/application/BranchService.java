package com.erp.core.admin.internal.company.application;

import com.erp.core.admin.internal.company.adapter.out.jpa.*;
import com.erp.core.admin.internal.company.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BranchService {
    private final BranchRepository branchRepo;
    private final CompanyRepository companyRepo;

    public BranchService(BranchRepository b, CompanyRepository c) {
        this.branchRepo = b;
        this.companyRepo = c;
    }

    @Transactional(readOnly = true)
    public List<Branch> getBranches(UUID tenantId, String filter) {
        List<BranchEntity> list = branchRepo.findByTenantId(tenantId);
        if ("active".equals(filter)) return list.stream().filter(BranchEntity::isActive).map(BranchEntity::toDomain).toList();
        if ("deleted".equals(filter)) return list.stream().filter(e -> !e.isActive()).map(BranchEntity::toDomain).toList();
        return list.stream().map(BranchEntity::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public List<Company> getCompanies(UUID tenantId) {
        return companyRepo.findByTenantId(tenantId).stream().filter(CompanyEntity::isActive).map(CompanyEntity::toDomain).toList();
    }

    @Transactional
    public Branch createBranch(UUID tenantId, UUID companyId, String name, String code) {
        Branch b = new Branch(UUID.randomUUID(), tenantId, companyId, name, code, null, null, true, Instant.now());
        branchRepo.save(BranchEntity.fromDomain(b));
        return b;
    }

    @Transactional(readOnly = true)
    public Branch getById(UUID id) {
        return branchRepo.findById(id).map(BranchEntity::toDomain).orElseThrow(() -> new IllegalArgumentException("Not found"));
    }

    @Transactional
    public Branch updateBranch(UUID id, UUID companyId, String name, String code) {
        BranchEntity e = branchRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        e.update(companyId, name, code);
        branchRepo.save(e);
        return e.toDomain();
    }

    @Transactional
    public void softDeleteBranch(UUID id) {
        BranchEntity e = branchRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        e.setActive(false);
        branchRepo.save(e);
    }

    @Transactional
    public void restoreBranch(UUID id) {
        BranchEntity e = branchRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        e.setActive(true);
        branchRepo.save(e);
    }

    @Transactional
    public void hardDeleteBranch(UUID id) {
        branchRepo.deleteById(id);
    }
}