package com.erp.core.admin.web;

import com.erp.core.admin.adapter.out.jpa.RoleEntity;
import com.erp.core.admin.adapter.out.jpa.SpringDataAdminRoleRepository;
import com.erp.core.admin.application.AdminRoleService;
import com.erp.core.admin.web.dto.CreateRoleRequest;
import com.erp.core.admin.web.dto.RoleSummary;
import com.erp.core.admin.web.dto.UpdateRoleRequest;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Session-based UI for role management.
 *
 * SECURITY:
 * - @PreAuthorize: only callers with ADMIN_ROLE_MANAGE authority may enter.
 * - Tenant is resolved from CurrentActorProvider — no hardcoded tenant.
 * - Cross-tenant role access is blocked explicitly.
 * - All state-changing endpoints are CSRF-protected by Spring Security's
 *   browser filter chain (HTMX auto-adds X-CSRF-TOKEN via layout.js).
 */
@Controller
@RequestMapping("/admin/settings/roles")
@PreAuthorize("hasAuthority('ADMIN_ROLE_MANAGE')")
public class RoleWebController {

    private final AdminRoleService service;
    private final SpringDataAdminRoleRepository roleRepo;
    private final PermissionRepositoryPort permissionRepo;
    private final CurrentActorProvider actorProvider;

    public RoleWebController(AdminRoleService service,
                             SpringDataAdminRoleRepository roleRepo,
                             PermissionRepositoryPort permissionRepo,
                             CurrentActorProvider actorProvider) {
        this.service = service;
        this.roleRepo = roleRepo;
        this.permissionRepo = permissionRepo;
        this.actorProvider = actorProvider;
    }

    private UUID tenantId() {
        CurrentActor a = actorProvider.getCurrentActor();
        if (a == null || a.tenantId() == null) {
            throw new IllegalStateException("No tenant context");
        }
        return a.tenantId();
    }

    private RoleEntity requireRoleInTenant(UUID roleId) {
        RoleEntity e = roleRepo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        if (!e.getTenantId().equals(tenantId())) {
            throw new IllegalStateException("Cross-tenant role access denied");
        }
        return e;
    }

    @GetMapping
    public String list(HttpServletRequest request, Model model,
                       @RequestParam(defaultValue = "all") String filter) {
        List<RoleSummary> all = service.list(tenantId());
        if ("active".equals(filter)) {
            all = all.stream().filter(RoleSummary::isActive).toList();
        } else if ("deleted".equals(filter)) {
            all = all.stream().filter(r -> !r.isActive()).toList();
        }
        model.addAttribute("roles", all);
        model.addAttribute("filter", filter);
        if (request.getHeader("HX-Request") != null) return "admin/settings/roles/index :: content";
        return "admin/settings/roles/index";
    }

    @GetMapping("/new")
    public String newForm() { return "admin/settings/roles/form :: form-content"; }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(defaultValue = "10") Integer level,
                         Model model) {
        service.create(new CreateRoleRequest(tenantId(), name, level));
        model.addAttribute("roles", service.list(tenantId()));
        model.addAttribute("filter", "all");
        return "admin/settings/roles/index :: content";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        requireRoleInTenant(id);
        RoleSummary r = service.list(tenantId()).stream()
                .filter(x -> x.id().equals(id)).findFirst().orElseThrow();
        model.addAttribute("r", r);
        return "admin/settings/roles/form :: form-content";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id,
                         @RequestParam String name,
                         @RequestParam(defaultValue = "10") Integer level,
                         Model model) {
        requireRoleInTenant(id);
        service.update(id, new UpdateRoleRequest(name, level));
        model.addAttribute("r", service.list(tenantId()).stream()
                .filter(x -> x.id().equals(id)).findFirst().orElseThrow());
        return "admin/settings/roles/row :: row";
    }

    @PostMapping("/{id}/soft-delete")
    public String softDelete(@PathVariable UUID id, Model model) {
        RoleEntity e = requireRoleInTenant(id);
        e.setActive(false);
        roleRepo.save(e);
        model.addAttribute("r", service.list(tenantId()).stream()
                .filter(x -> x.id().equals(id)).findFirst().orElseThrow());
        return "admin/settings/roles/row :: row";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable UUID id, Model model) {
        RoleEntity e = requireRoleInTenant(id);
        e.setActive(true);
        roleRepo.save(e);
        model.addAttribute("r", service.list(tenantId()).stream()
                .filter(x -> x.id().equals(id)).findFirst().orElseThrow());
        return "admin/settings/roles/row :: row";
    }

    @DeleteMapping("/{id}")
    public String hardDelete(@PathVariable UUID id) {
        requireRoleInTenant(id);
        service.delete(id);
        return "admin/settings/roles/row :: empty";
    }

    // ---- Manage permissions (modal) ----

    @GetMapping("/{id}/manage-permissions")
    public String managePermissions(@PathVariable UUID id, Model model) {
        requireRoleInTenant(id);
        List<PermissionDefinition> all = permissionRepo.findAll().stream()
                .sorted(Comparator.comparing(PermissionDefinition::group)
                        .thenComparing(p -> p.key().value()))
                .toList();
        model.addAttribute("roleId", id);
        model.addAttribute("allPermissions", all);
        model.addAttribute("assignedPermissions", service.getAssignedPermissions(id));
        return "admin/settings/roles/manage-permissions :: form-content";
    }

    @PostMapping("/{id}/manage-permissions")
    public String savePermissions(@PathVariable UUID id,
                                  @RequestParam(required = false) List<String> permissionKeys,
                                  HttpServletResponse response) {
        requireRoleInTenant(id);
        service.replacePermissions(id, permissionKeys);
        response.setHeader("HX-Trigger", "closeModal");
        return "admin/settings/roles/row :: empty";
    }

    /**
     * Session-based, CSRF-protected toggle for the inline matrix UI.
     * Uses the browser's session, NOT the JWT API. This closes the auth gap
     * previously present in /api/admin/roles/{id}/permissions/{key}.
     */
    @PostMapping("/{id}/toggle-permission")
    public String togglePermission(@PathVariable UUID id,
                                   @RequestParam String permissionKey,
                                   @RequestParam(defaultValue = "false") boolean grant,
                                   HttpServletResponse response) {
        requireRoleInTenant(id);
        service.togglePermission(id, permissionKey, grant);
        response.setHeader("HX-Trigger", "permToggled");
        return "admin/settings/roles/row :: empty";
    }
}