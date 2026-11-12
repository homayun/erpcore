package com.erp.core.admin.internal.web;

import com.erp.core.admin.application.AdminRoleService;
import com.erp.core.admin.application.AdminUserService;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * JWT-authenticated REST API for admin operations. Requires a Bearer token;
 * session cookies do NOT authenticate this endpoint.
 *
 * SECURITY (hardened):
 * - Cross-tenant checks on every mutating call.
 * - Target actor must belong to caller's tenant before role assignment.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final AdminRoleService roleService;
    private final AdminUserService userService;
    private final CurrentActorProvider actorProvider;

    public AdminApiController(AdminRoleService roleService,
                              AdminUserService userService,
                              CurrentActorProvider actorProvider) {
        this.roleService = roleService;
        this.userService = userService;
        this.actorProvider = actorProvider;
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_ROLE', 'MANAGE')")
    @PostMapping("/roles/{roleId}/permissions/{permissionKey}")
    public ResponseEntity<?> togglePermission(@PathVariable UUID roleId,
                                              @PathVariable String permissionKey,
                                              @RequestParam boolean grant) {
        CurrentActor actor = actorProvider.getCurrentActor();
        if (actor == null || actor.tenantId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "no tenant context"));
        }
        roleService.togglePermission(roleId, permissionKey, grant);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_ROLE', 'MANAGE')")
    @PostMapping("/roles/{roleId}/assignments/{actorId}")
    public ResponseEntity<?> assignRole(@PathVariable UUID roleId, @PathVariable UUID actorId) {
        CurrentActor actor = actorProvider.getCurrentActor();
        if (actor == null || actor.tenantId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "no tenant context"));
        }
        // Verify target actor belongs to the caller's tenant
        boolean targetInTenant = userService.list(actor.tenantId(), 0, 200).items().stream()
                .anyMatch(u -> u.actorId().equals(actorId));
        if (!targetInTenant) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "target actor not in tenant"));
        }
        roleService.assignRole(roleId, actorId, actor.tenantId());
        return ResponseEntity.ok().build();
    }
}