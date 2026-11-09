package com.erp.core.admin.application;

import com.erp.core.admin.port.AdminRolePort;
import com.erp.core.admin.web.dto.CreateRoleRequest;
import com.erp.core.admin.web.dto.RoleSummary;
import com.erp.core.admin.web.dto.UpdateRoleRequest;

import java.util.List;
import java.util.UUID;

public class AdminRoleService {

    private final AdminRolePort port;

    public AdminRoleService(AdminRolePort port) {
        this.port = port;
    }

    public RoleSummary create(CreateRoleRequest req) {
        if (req.tenantId() == null) throw new IllegalArgumentException("tenantId is required");
        if (req.name() == null || req.name().isBlank()) throw new IllegalArgumentException("name is required");
        int level = req.hierarchyLevel() == null ? 10 : req.hierarchyLevel();
        UUID roleId = port.createRole(req.tenantId(), req.name(), level);
        return new RoleSummary(roleId, req.tenantId(), req.name(), level, java.util.Set.of(), true);
    }

    public List<RoleSummary> list(UUID tenantId) {
        return port.listRoles(tenantId).stream()
                .map(v -> new RoleSummary(v.id(), v.tenantId(), v.name(), v.hierarchyLevel(), v.permissionKeys(), v.isActive()))
                .toList();
    }

    public void grantPermission(UUID roleId, String permissionKey) {
        port.grantPermission(roleId, permissionKey);
    }

    public void revokePermission(UUID roleId, String permissionKey) {
        port.revokePermission(roleId, permissionKey);
    }

    public void assignRole(UUID roleId, UUID actorId, UUID tenantId) {
        port.assignRole(actorId, roleId, tenantId);
    }

    public void update(UUID roleId, UpdateRoleRequest req) {
        if (req.name() == null || req.name().isBlank()) throw new IllegalArgumentException("name is required");
        int level = req.hierarchyLevel() == null ? 10 : req.hierarchyLevel();
        port.updateRole(roleId, req.name(), level);
    }

    public void delete(UUID roleId) {
        port.deleteRole(roleId);
    }
}