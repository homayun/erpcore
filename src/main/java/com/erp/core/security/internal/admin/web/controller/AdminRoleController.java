package com.erp.core.security.internal.admin.web.controller;

import com.erp.core.security.internal.admin.application.AdminRoleService;
import com.erp.core.security.internal.admin.web.dto.AssignRoleRequest;
import com.erp.core.security.internal.admin.web.dto.CreateRoleRequest;
import com.erp.core.security.internal.admin.web.dto.RoleSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/roles")
public class AdminRoleController {

    private final AdminRoleService service;

    public AdminRoleController(AdminRoleService service) {
        this.service = service;
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_ROLE', 'MANAGE')")
    @PostMapping
    public ResponseEntity<RoleSummary> create(@RequestBody CreateRoleRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_ROLE', 'MANAGE')")
    @GetMapping
    public List<RoleSummary> list(@RequestParam UUID tenantId) {
        return service.list(tenantId);
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_ROLE', 'MANAGE')")
    @PostMapping("/{roleId}/permissions/{permissionKey}")
    public ResponseEntity<Void> grant(@PathVariable UUID roleId, @PathVariable String permissionKey) {
        service.grantPermission(roleId, permissionKey);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_ROLE', 'MANAGE')")
    @DeleteMapping("/{roleId}/permissions/{permissionKey}")
    public ResponseEntity<Void> revoke(@PathVariable UUID roleId, @PathVariable String permissionKey) {
        service.revokePermission(roleId, permissionKey);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_ROLE', 'MANAGE')")
    @PostMapping("/{roleId}/assignments")
    public ResponseEntity<Void> assign(@PathVariable UUID roleId, @RequestBody AssignRoleRequest req) {
        service.assignRole(roleId, req.actorId(), req.tenantId());
        return ResponseEntity.noContent().build();
    }
}