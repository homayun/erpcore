package com.erp.core.admin.internal.company.adapter.out.jpa;

import com.erp.core.admin.internal.company.domain.Company;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_company")
public class CompanyEntity {
    @Id 
    private UUID id;
    
    @Column(name = "tenant_id") 
    private UUID tenantId;
    
    @Column(name = "master_company_id")
    private UUID masterCompanyId;
    
    private String name;
    
    @Column(name = "registration_number") 
    private String registrationNumber;
    
    @Column(name = "tax_id") 
    private String taxId;
    
    private String email;
    private String phone;
    private String address;
    
    @Column(name = "is_active") 
    private boolean isActive;
    
    @Column(name = "created_at") 
    private Instant createdAt;

    public CompanyEntity() {}

    public static CompanyEntity fromDomain(Company d) {
        CompanyEntity e = new CompanyEntity();
        e.id = d.id();
        e.tenantId = d.tenantId();
        e.masterCompanyId = d.masterCompanyId();
        e.name = d.name();
        e.registrationNumber = d.registrationNumber();
        e.taxId = d.taxId();
        e.email = d.email();
        e.phone = d.phone();
        e.address = d.address();
        e.isActive = d.isActive();
        e.createdAt = d.createdAt();
        return e;
    }

    public Company toDomain() {
        return new Company(id, tenantId, masterCompanyId, name, registrationNumber, taxId, email, phone, address, isActive, createdAt);
    }
}