package com.erp.core.admin.internal.company.adapter.out.jpa;
import com.erp.core.admin.internal.company.domain.MasterCompany;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "master_companies")
public class MasterCompanyEntity {
    @Id private UUID id;
    @Column(name = "tenant_id") private UUID tenantId;
    private String name;
    private String code;
    @Column(name = "is_active") private boolean isActive;
    @Column(name = "created_at") private Instant createdAt;

    public MasterCompanyEntity() {}

    public static MasterCompanyEntity fromDomain(MasterCompany d) {
        MasterCompanyEntity e = new MasterCompanyEntity();
        e.id = d.id(); e.tenantId = d.tenantId(); e.name = d.name(); e.code = d.code(); e.isActive = d.isActive(); e.createdAt = d.createdAt();
        return e;
    }
    public MasterCompany toDomain() {
        return new MasterCompany(id, tenantId, name, code, isActive, createdAt);
    }
}