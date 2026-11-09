package com.erp.core.security.internal.authorization.adapter.out.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sec_user_permission")
@IdClass(UserPermissionEntity.Key.class)
public class UserPermissionEntity {

    @Id
    @Column(name = "actor_id")
    private UUID actorId;

    @Id
    @Column(name = "permission_key", length = 100)
    private String permissionKey;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    protected UserPermissionEntity() {}

    public UserPermissionEntity(UUID actorId, String permissionKey, UUID tenantId) {
        this.actorId = actorId;
        this.permissionKey = permissionKey;
        this.tenantId = tenantId;
    }

    public UUID getActorId() { return actorId; }
    public String getPermissionKey() { return permissionKey; }
    public UUID getTenantId() { return tenantId; }

    public static final class Key implements Serializable {
        private UUID actorId;
        private String permissionKey;
        public Key() {}
        public Key(UUID actorId, String permissionKey) {
            this.actorId = actorId;
            this.permissionKey = permissionKey;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Key k)) return false;
            return Objects.equals(actorId, k.actorId)
                    && Objects.equals(permissionKey, k.permissionKey);
        }
        @Override public int hashCode() { return Objects.hash(actorId, permissionKey); }
    }
}