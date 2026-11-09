package com.erp.core.admin.internal.company.application;

import com.erp.core.admin.internal.company.adapter.out.jpa.*;
import com.erp.core.admin.internal.company.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MasterCompanyService {
    private final MasterCompanyRepository masterRepo;
    private final CompanyRepository companyRepo;

    public MasterCompanyService(MasterCompanyRepository masterRepo, CompanyRepository companyRepo) {
        this.masterRepo = masterRepo;
        this.companyRepo = companyRepo;
    }

    @Transactional(readOnly = true)
    public List<MasterCompany> getAll(UUID tenantId, String filter) {
        List<MasterCompanyEntity> list = masterRepo.findByTenantId(tenantId);
        if ("active".equals(filter)) return list.stream().filter(MasterCompanyEntity::isActive).map(MasterCompanyEntity::toDomain).toList();
        if ("deleted".equals(filter)) return list.stream().filter(e -> !e.isActive()).map(MasterCompanyEntity::toDomain).toList();
        return list.stream().map(MasterCompanyEntity::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public MasterCompany getById(UUID id) {
        return masterRepo.findById(id).map(MasterCompanyEntity::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Master Company not found"));
    }

    @Transactional
    public MasterCompany create(UUID tenantId, String name, String code) {
        MasterCompany mc = new MasterCompany(UUID.randomUUID(), tenantId, name, code, true, Instant.now());
        masterRepo.save(MasterCompanyEntity.fromDomain(mc));
        return mc;
    }

    @Transactional
    public MasterCompany update(UUID id, String name, String code) {
        MasterCompanyEntity e = masterRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        e.update(name, code);
        masterRepo.save(e);
        return e.toDomain();
    }

    @Transactional
    public void softDelete(UUID id) {
        if (companyRepo.existsByMasterCompanyId(id)) {
            throw new IllegalStateException("Cannot soft delete! This group contains active Companies.");
        }
        MasterCompanyEntity e = masterRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        e.setActive(false);
        masterRepo.save(e);
    }

    @Transactional
    public void restore(UUID id) {
        MasterCompanyEntity e = masterRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        e.setActive(true);
        masterRepo.save(e);
    }

    @Transactional
    public void hardDelete(UUID id) {
        if (companyRepo.existsByMasterCompanyId(id)) {
            throw new IllegalStateException("Cannot permanently delete! This group contains Companies.");
        }
        masterRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public void exportCsv(UUID tenantId, PrintWriter writer) {
        writer.println("Code,Name,Status");
        List<MasterCompanyEntity> list = masterRepo.findByTenantId(tenantId);
        for (MasterCompanyEntity e : list) {
            writer.println(e.toDomain().code() + "," + e.toDomain().name() + "," + (e.isActive() ? "Active" : "Deleted"));
        }
    }

    @Transactional
    public void importCsv(UUID tenantId, MultipartFile file) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirst = true;
            while ((line = br.readLine()) != null) {
                if (isFirst) { isFirst = false; continue; } // Skip header
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String code = parts[0].trim();
                    String name = parts[1].trim();
                    MasterCompany mc = new MasterCompany(UUID.randomUUID(), tenantId, name, code, true, Instant.now());
                    masterRepo.save(MasterCompanyEntity.fromDomain(mc));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV: " + e.getMessage());
        }
    }
}