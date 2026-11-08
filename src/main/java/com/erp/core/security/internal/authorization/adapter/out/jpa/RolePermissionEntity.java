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
@Table(name = "sec_role_permission")
@IdClass(RolePermissionEntity.Key.class)
public class RolePermissionEntity {

    @Id
    @Column(name = "role_id")
    private UUID roleId;

    @Id
    @Column(name = "permission_key", length = 100)
    private String permissionKey;

    protected RolePermissionEntity() {}

    public RolePermissionEntity(UUID roleId, String permissionKey) {
        this.roleId = roleId;
        this.permissionKey = permissionKey;
    }

    public UUID getRoleId() { return roleId; }
    public String getPermissionKey() { return permissionKey; }

    public static final class Key implements Serializable {
        private UUID roleId;
        private String permissionKey;
        public Key() {}
        public Key(UUID roleId, String permissionKey) {
            this.roleId = roleId;
            this.permissionKey = permissionKey;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Key k)) return false;
            return Objects.equals(roleId, k.roleId)
                    && Objects.equals(permissionKey, k.permissionKey);
        }
        @Override public int hashCode() { return Objects.hash(roleId, permissionKey); }
    }
}