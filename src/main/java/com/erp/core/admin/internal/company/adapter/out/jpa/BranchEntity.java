package com.erp.core.admin.internal.company.adapter.out.jpa;
import com.erp.core.admin.internal.company.domain.Branch;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "admin_branch")
public class BranchEntity {
    @Id private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private UUID companyId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String branchCode;
    @Column(columnDefinition = "TEXT") private String address;
    private String phone;
    private boolean isActive;
    private Instant createdAt;

    protected BranchEntity() {}

    public static BranchEntity fromDomain(Branch b) {
        BranchEntity e = new BranchEntity();
        e.id = b.id(); e.tenantId = b.tenantId(); e.companyId = b.companyId();
        e.name = b.name(); e.branchCode = b.branchCode(); e.address = b.address();
        e.phone = b.phone(); e.isActive = b.isActive(); e.createdAt = b.createdAt();
        return e;
    }

    public void update(UUID companyId, String name, String code) {
        this.companyId = companyId; this.name = name; this.branchCode = code;
    }

    public void setActive(boolean active) { this.isActive = active; }
    public boolean isActive() { return isActive; }

    public Branch toDomain() { return new Branch(id, tenantId, companyId, name, branchCode, address, phone, isActive, createdAt); }
}