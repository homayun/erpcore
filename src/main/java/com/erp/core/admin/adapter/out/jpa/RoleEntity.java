package com.erp.core.admin.adapter.out.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sec_role")
public class RoleEntity {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "hierarchy_level", nullable = false) private int hierarchyLevel;
    @Column(name = "is_active", nullable = false) private boolean isActive = true;

    protected RoleEntity() {}

    public static RoleEntity of(UUID id, UUID tenantId, String name, int level) {
        RoleEntity e = new RoleEntity();
        e.id = id; e.tenantId = tenantId; e.name = name; e.hierarchyLevel = level; e.isActive = true;
        return e;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public int getHierarchyLevel() { return hierarchyLevel; }
    public boolean isActive() { return isActive; }
    
    public void setName(String name) { this.name = name; }
    public void setHierarchyLevel(int hierarchyLevel) { this.hierarchyLevel = hierarchyLevel; }
    public void setActive(boolean active) { this.isActive = active; }
}