package com.erp.core.security.internal.admin.port;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AdminRolePort {

    record RoleView(UUID id, UUID tenantId, String name, int hierarchyLevel,
                    Set<String> permissionKeys) {}

    /** Idempotent - creates if absent, returns existing id otherwise. */
    UUID ensureRole(UUID tenantId, String name, int hierarchyLevel);

    /** Strict - throws IllegalStateException if role name already exists. */
    UUID createRole(UUID tenantId, String name, int hierarchyLevel);

    void grantPermission(UUID roleId, String permissionKey);

    void revokePermission(UUID roleId, String permissionKey);

    void ensureAssignment(UUID actorId, UUID roleId, UUID tenantId);

    void assignRole(UUID actorId, UUID roleId, UUID tenantId);

    List<RoleView> listRoles(UUID tenantId);
}