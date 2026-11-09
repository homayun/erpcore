package com.erp.core.security.internal.authorization.adapter.out.jpa;

import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sec_permission")
public class PermissionEntity {

    @Id
    @Column(name = "permission_key", length = 100)
    private String permissionKey;

    @Column(name = "group_key", nullable = false, length = 50)
    private String groupKey;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "is_sensitive", nullable = false)
    private boolean sensitive;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected PermissionEntity() {}

    public static PermissionEntity fromDomain(PermissionDefinition d) {
        PermissionEntity e = new PermissionEntity();
        e.permissionKey = d.key().value();
        e.groupKey = d.group();
        e.description = d.description();
        e.sensitive = d.sensitive();
        e.isActive = true;
        return e;
    }

    public PermissionDefinition toDomain() {
        return new PermissionDefinition(
                PermissionKey.of(permissionKey), groupKey, description, sensitive);
    }

    public String getPermissionKey() { return permissionKey; }
    public String getGroupKey() { return groupKey; }
    public String getDescription() { return description; }
    public boolean isSensitive() { return sensitive; }
    public boolean isActive() { return isActive; }

    public void setGroupKey(String v) { this.groupKey = v; }
    public void setDescription(String v) { this.description = v; }
    public void setSensitive(boolean v) { this.sensitive = v; }
    public void setActive(boolean v) { this.isActive = v; }
}