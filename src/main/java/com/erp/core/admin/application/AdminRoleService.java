package com.erp.core.admin.application;

import com.erp.core.admin.port.AdminRolePort;
import com.erp.core.admin.web.dto.CreateRoleRequest;
import com.erp.core.admin.web.dto.RoleSummary;
import com.erp.core.admin.web.dto.UpdateRoleRequest;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminRoleService {
    private final AdminRolePort rolePort;
    private final PermissionRepositoryPort permissionRepo;

    public AdminRoleService(AdminRolePort rolePort, PermissionRepositoryPort permissionRepo) {
        this.rolePort = rolePort;
        this.permissionRepo = permissionRepo;
    }

    public RoleSummary create(CreateRoleRequest req) {
        if (req.tenantId() == null) throw new IllegalArgumentException("tenantId is required");
        if (req.name() == null || req.name().isBlank()) throw new IllegalArgumentException("name is required");
        int level = req.hierarchyLevel() == null ? 10 : req.hierarchyLevel();
        UUID roleId = rolePort.createRole(req.tenantId(), req.name(), level);
        return new RoleSummary(roleId, req.tenantId(), req.name(), level, java.util.Set.of(), true);
    }

    public List<RoleSummary> list(UUID tenantId) {
        return rolePort.listRoles(tenantId).stream()
                .map(v -> new RoleSummary(v.id(), v.tenantId(), v.name(), v.hierarchyLevel(), v.permissionKeys(), v.isActive()))
                .collect(Collectors.toList());
    }

    public List<RoleSummary> getRoles(UUID tenantId) {
        return list(tenantId);
    }

    public void update(UUID roleId, UpdateRoleRequest req) {
        if (req.name() == null || req.name().isBlank()) throw new IllegalArgumentException("name is required");
        int level = req.hierarchyLevel() == null ? 10 : req.hierarchyLevel();
        rolePort.updateRole(roleId, req.name(), level);
    }

    public void delete(UUID roleId) {
        rolePort.deleteRole(roleId);
    }

    public void grantPermission(UUID roleId, String permissionKey) {
        rolePort.grantPermission(roleId, permissionKey);
    }

    public void revokePermission(UUID roleId, String permissionKey) {
        rolePort.revokePermission(roleId, permissionKey);
    }

    public void togglePermission(UUID roleId, String permissionKey, boolean grant) {
        if (grant) {
            rolePort.grantPermission(roleId, permissionKey);
        } else {
            rolePort.revokePermission(roleId, permissionKey);
        }
    }

    public void assignRole(UUID roleId, UUID actorId, UUID tenantId) {
        rolePort.assignRole(actorId, roleId, tenantId);
    }

    public List<String> getAllPermissions() {
        return permissionRepo.findAll().stream().map(p -> p.key().value()).collect(Collectors.toList());
    }

    public List<String> getAssignedPermissions(UUID roleId) {
        return rolePort.getPermissionsForRole(roleId).stream().toList();
    }

    public void replacePermissions(UUID roleId, List<String> permissions) {
        rolePort.replacePermissions(roleId, permissions);
    }
}