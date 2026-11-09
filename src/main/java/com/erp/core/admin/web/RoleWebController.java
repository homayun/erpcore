package com.erp.core.admin.web;

import com.erp.core.admin.adapter.out.jpa.SpringDataAdminRoleRepository;
import com.erp.core.admin.application.AdminRoleService;
import com.erp.core.admin.web.dto.CreateRoleRequest;
import com.erp.core.admin.web.dto.RoleSummary;
import com.erp.core.admin.web.dto.UpdateRoleRequest;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/roles")
public class RoleWebController {
    private final AdminRoleService service;
    private final SpringDataAdminRoleRepository roleRepo;
    private final PermissionRepositoryPort permissionRepo;
    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public RoleWebController(AdminRoleService service,
                             SpringDataAdminRoleRepository roleRepo,
                             PermissionRepositoryPort permissionRepo) {
        this.service = service;
        this.roleRepo = roleRepo;
        this.permissionRepo = permissionRepo;
    }

    @GetMapping
    public String list(HttpServletRequest request, Model model, @RequestParam(defaultValue = "all") String filter) {
        List<RoleSummary> all = service.list(tenantId);
        if ("active".equals(filter)) all = all.stream().filter(r -> roleRepo.findById(r.id()).map(e -> e.isActive()).orElse(false)).toList();
        else if ("deleted".equals(filter)) all = all.stream().filter(r -> !roleRepo.findById(r.id()).map(e -> e.isActive()).orElse(true)).toList();

        model.addAttribute("roles", all);
        model.addAttribute("filter", filter);
        if (request.getHeader("HX-Request") != null) return "admin/settings/roles/index :: content";
        return "admin/settings/roles/index";
    }

    @GetMapping("/new")
    public String newForm() { return "admin/settings/roles/form :: form-content"; }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam int level, Model model) {
        service.create(new CreateRoleRequest(tenantId, name, level));
        model.addAttribute("roles", service.list(tenantId));
        model.addAttribute("filter", "all");
        return "admin/settings/roles/index :: content";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id, @RequestParam String name, @RequestParam int level, Model model) {
        service.update(id, new UpdateRoleRequest(name, level));
        model.addAttribute("r", service.list(tenantId).stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow());
        return "admin/settings/roles/row :: row";
    }

    @PostMapping("/{id}/soft-delete")
    public String softDelete(@PathVariable UUID id, Model model) {
        roleRepo.findById(id).ifPresent(e -> { e.setActive(false); roleRepo.save(e); });
        model.addAttribute("r", service.list(tenantId).stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow());
        return "admin/settings/roles/row :: row";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable UUID id, Model model) {
        roleRepo.findById(id).ifPresent(e -> { e.setActive(true); roleRepo.save(e); });
        model.addAttribute("r", service.list(tenantId).stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow());
        return "admin/settings/roles/row :: row";
    }

    @DeleteMapping("/{id}")
    public String hardDelete(@PathVariable UUID id) {
        service.delete(id);
        return "admin/settings/roles/row :: empty";
    }

    // ---- NEW: Role -> Permission assignment ----
    @GetMapping("/{id}/manage-permissions")
    public String managePermissions(@PathVariable UUID id, Model model) {
        List<com.erp.core.security.internal.authorization.domain.PermissionDefinition> all =
                permissionRepo.findAll().stream()
                        .sorted(Comparator.comparing(
                                (com.erp.core.security.internal.authorization.domain.PermissionDefinition p) -> p.group())
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
        service.replacePermissions(id, permissionKeys);
        response.setHeader("HX-Trigger", "closeModal");
        return "admin/settings/roles/row :: empty";
    }
}