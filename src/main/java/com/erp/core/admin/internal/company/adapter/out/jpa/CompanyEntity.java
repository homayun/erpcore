package com.erp.core.admin.internal.company.adapter.out.jpa;
import com.erp.core.admin.internal.company.domain.Company;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "admin_company")
public class CompanyEntity {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private String name;
    private String registrationNumber;
    private String taxId;
    private String email;
    private String phone;
    @Column(columnDefinition = "TEXT") private String address;
    private boolean isActive;
    private Instant createdAt;
    protected CompanyEntity() {}
    public static CompanyEntity fromDomain(Company c) {
        CompanyEntity e = new CompanyEntity();
        e.id = c.id(); e.tenantId = c.tenantId(); e.name = c.name();
        e.registrationNumber = c.registrationNumber(); e.taxId = c.taxId();
        e.email = c.email(); e.phone = c.phone(); e.address = c.address();
        e.isActive = c.isActive(); e.createdAt = c.createdAt();
        return e;
    }
    public Company toDomain() { return new Company(id, tenantId, name, registrationNumber, taxId, email, phone, address, isActive, createdAt); }
}