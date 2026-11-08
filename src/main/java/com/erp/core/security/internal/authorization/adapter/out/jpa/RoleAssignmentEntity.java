package com.erp.core.security.internal.authorization.adapter.out.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sec_role_assignment")
public class RoleAssignmentEntity {

    @Id
    private UUID id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    protected RoleAssignmentEntity() {}

    public static RoleAssignmentEntity of(UUID id, UUID actorId, UUID roleId, UUID tenantId) {
        RoleAssignmentEntity e = new RoleAssignmentEntity();
        e.id = id; e.actorId = actorId; e.roleId = roleId; e.tenantId = tenantId;
        return e;
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public UUID getRoleId() { return roleId; }
    public UUID getTenantId() { return tenantId; }
}