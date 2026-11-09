package com.erp.core.admin.port;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AdminRolePort {

    record RoleView(UUID id, UUID tenantId, String name, int hierarchyLevel,
                    Set<String> permissionKeys, boolean isActive) {}

    UUID ensureRole(UUID tenantId, String name, int hierarchyLevel);
    UUID createRole(UUID tenantId, String name, int hierarchyLevel);
    void grantPermission(UUID roleId, String permissionKey);
    void revokePermission(UUID roleId, String permissionKey);
    void ensureAssignment(UUID actorId, UUID roleId, UUID tenantId);
    void assignRole(UUID actorId, UUID roleId, UUID tenantId);
    List<RoleView> listRoles(UUID tenantId);

    void updateRole(UUID roleId, String newName, int hierarchyLevel);
    void deleteRole(UUID roleId);
}