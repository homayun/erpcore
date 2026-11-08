package com.erp.core.security.internal.admin.adapter.out.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sec_role")
public class RoleEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "hierarchy_level", nullable = false)
    private int hierarchyLevel;

    protected RoleEntity() {}

    public static RoleEntity of(UUID id, UUID tenantId, String name, int level) {
        RoleEntity e = new RoleEntity();
        e.id = id; e.tenantId = tenantId; e.name = name; e.hierarchyLevel = level;
        return e;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public int getHierarchyLevel() { return hierarchyLevel; }
}