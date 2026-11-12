package com.erp.core.security.internal.tenant.adapter.out.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sec_actor_tenant_membership")
@IdClass(TenantMembershipEntity.Key.class)
public class TenantMembershipEntity {

    @Id
    @Column(name = "actor_id")
    private UUID actorId;

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    protected TenantMembershipEntity() {}

    public TenantMembershipEntity(UUID actorId, UUID tenantId) {
        this.actorId = actorId;
        this.tenantId = tenantId;
    }

    public UUID getActorId() { return actorId; }
    public UUID getTenantId() { return tenantId; }

    public static final class Key implements Serializable {
        private UUID actorId;
        private UUID tenantId;
        public Key() {}
        public Key(UUID actorId, UUID tenantId) {
            this.actorId = actorId;
            this.tenantId = tenantId;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Key k)) return false;
            return Objects.equals(actorId, k.actorId) && Objects.equals(tenantId, k.tenantId);
        }
        @Override public int hashCode() { return Objects.hash(actorId, tenantId); }
    }
}