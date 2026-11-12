package com.erp.core.admin.web;

import com.erp.core.admin.application.AdminUserService;
import com.erp.core.admin.web.dto.CreateUserRequest;
import com.erp.core.admin.web.dto.UserSummary;
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
 * Session-based UI for user management.
 *
 * SECURITY:
 * - @PreAuthorize: only callers with ADMIN_USER_MANAGE authority.
 * - Tenant resolved per-request (no hardcoded tenant).
 * - CSRF-protected via browser filter chain.
 */
@Controller
@RequestMapping("/admin/settings/users")
@PreAuthorize("hasAuthority('ADMIN_USER_MANAGE')")
public class UserWebController {

    private final AdminUserService service;
    private final PermissionRepositoryPort permissionRepo;
    private final CurrentActorProvider actorProvider;

    public UserWebController(AdminUserService service,
                             PermissionRepositoryPort permissionRepo,
                             CurrentActorProvider actorProvider) {
        this.service = service;
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

    private UserSummary requireUserInTenant(UUID actorId) {
        return service.list(tenantId(), 0, 200).items().stream()
                .filter(u -> u.actorId().equals(actorId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found in tenant"));
    }

    @GetMapping
    public String list(HttpServletRequest request, Model model,
                       @RequestParam(defaultValue = "all") String filter) {
        List<UserSummary> all = service.list(tenantId(), 0, 100).items();
        if ("active".equals(filter)) all = all.stream().filter(UserSummary::enabled).toList();
        else if ("deleted".equals(filter)) all = all.stream().filter(u -> !u.enabled()).toList();

        model.addAttribute("users", all);
        model.addAttribute("filter", filter);
        if (request.getHeader("HX-Request") != null) return "admin/settings/users/index :: content";
        return "admin/settings/users/index";
    }

    @GetMapping("/new")
    public String newForm() { return "admin/settings/users/form :: form-content"; }

    @PostMapping
    public String create(@RequestParam String username, @RequestParam String password, Model model) {
        service.create(new CreateUserRequest(tenantId(), username, password));
        model.addAttribute("users", service.list(tenantId(), 0, 100).items());
        model.addAttribute("filter", "all");
        return "admin/settings/users/index :: content";
    }

    @PostMapping("/{id}/soft-delete")
    public String softDelete(@PathVariable UUID id, Model model) {
        requireUserInTenant(id);
        service.setEnabled(id, tenantId(), false);
        model.addAttribute("u", service.list(tenantId(), 0, 100).items().stream()
                .filter(x -> x.actorId().equals(id)).findFirst().orElseThrow());
        return "admin/settings/users/row :: row";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable UUID id, Model model) {
        requireUserInTenant(id);
        service.setEnabled(id, tenantId(), true);
        model.addAttribute("u", service.list(tenantId(), 0, 100).items().stream()
                .filter(x -> x.actorId().equals(id)).findFirst().orElseThrow());
        return "admin/settings/users/row :: row";
    }

    @DeleteMapping("/{id}")
    public String hardDelete(@PathVariable UUID id) {
        requireUserInTenant(id);
        service.delete(id, tenantId());
        return "admin/settings/users/row :: empty";
    }

    // ---- Manage Roles ----

    @GetMapping("/{id}/manage-roles")
    public String manageRoles(@PathVariable UUID id, Model model) {
        requireUserInTenant(id);
        model.addAttribute("userId", id);
        model.addAttribute("allRoles", service.getRoles(tenantId()));
        model.addAttribute("assignedRoles", service.getAssignedRoles(id, tenantId()));
        return "admin/settings/users/manage-roles :: form-content";
    }

    @PostMapping("/{id}/manage-roles")
    public String saveRoles(@PathVariable UUID id,
                            @RequestParam(required = false) List<UUID> roleIds,
                            HttpServletResponse response) {
        requireUserInTenant(id);
        service.assignRoles(id, tenantId(), roleIds);
        response.setHeader("HX-Trigger", "closeModal");
        return "admin/settings/users/row :: empty";
    }

    /**
     * Session-based additive role assignment (inline form on /admin/settings/user-role).
     * CSRF-protected.
     */
    @PostMapping("/{id}/assign-role")
    public String assignRole(@PathVariable UUID id,
                             @RequestParam UUID roleId,
                             HttpServletResponse response) {
        requireUserInTenant(id);
        service.addRole(id, tenantId(), roleId);
        response.setHeader("HX-Redirect", "/admin/settings/user-role");
        return "admin/settings/users/row :: empty";
    }

    // ---- Manage Direct Permissions ----

    @GetMapping("/{id}/manage-permissions")
    public String managePermissions(@PathVariable UUID id, Model model) {
        requireUserInTenant(id);
        List<PermissionDefinition> all = permissionRepo.findAll().stream()
                .sorted(Comparator.comparing(PermissionDefinition::group)
                        .thenComparing(p -> p.key().value()))
                .toList();
        model.addAttribute("userId", id);
        model.addAttribute("allPermissions", all);
        model.addAttribute("assignedPermissions", service.getAssignedDirectPermissions(id, tenantId()));
        return "admin/settings/users/manage-permissions :: form-content";
    }

    @PostMapping("/{id}/manage-permissions")
    public String savePermissions(@PathVariable UUID id,
                                  @RequestParam(required = false) List<String> permissionKeys,
                                  HttpServletResponse response) {
        requireUserInTenant(id);
        service.replaceDirectPermissions(id, tenantId(), permissionKeys);
        response.setHeader("HX-Trigger", "closeModal");
        return "admin/settings/users/row :: empty";
    }
}