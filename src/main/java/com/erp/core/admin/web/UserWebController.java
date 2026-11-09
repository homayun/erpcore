package com.erp.core.admin.web;

import com.erp.core.admin.application.AdminUserService;
import com.erp.core.admin.web.dto.CreateUserRequest;
import com.erp.core.admin.web.dto.UserSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/users")
public class UserWebController {
    private final AdminUserService service;
    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public UserWebController(AdminUserService service) { this.service = service; }

    @GetMapping
    public String list(HttpServletRequest request, Model model, @RequestParam(defaultValue = "all") String filter) {
        List<UserSummary> all = service.list(tenantId, 0, 100).items();
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
        service.create(new CreateUserRequest(tenantId, username, password));
        model.addAttribute("users", service.list(tenantId, 0, 100).items());
        model.addAttribute("filter", "all");
        return "admin/settings/users/index :: content";
    }

    @PostMapping("/{id}/soft-delete")
    public String softDelete(@PathVariable UUID id, Model model) {
        service.setEnabled(id, tenantId, false);
        model.addAttribute("u", service.list(tenantId, 0, 100).items().stream().filter(x -> x.actorId().equals(id)).findFirst().orElseThrow());
        return "admin/settings/users/row :: row";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable UUID id, Model model) {
        service.setEnabled(id, tenantId, true);
        model.addAttribute("u", service.list(tenantId, 0, 100).items().stream().filter(x -> x.actorId().equals(id)).findFirst().orElseThrow());
        return "admin/settings/users/row :: row";
    }

    @DeleteMapping("/{id}")
    public String hardDelete(@PathVariable UUID id) {
        service.delete(id, tenantId);
        return "admin/settings/users/row :: empty";
    }

    @GetMapping("/{id}/manage-roles")
    public String manageRoles(@PathVariable UUID id, Model model) {
        model.addAttribute("userId", id);
        model.addAttribute("allRoles", service.getRoles(tenantId));
        model.addAttribute("assignedRoles", service.getAssignedRoles(id, tenantId));
        return "admin/settings/users/manage-roles :: form-content";
    }

    @PostMapping("/{id}/manage-roles")
    public String saveRoles(@PathVariable UUID id, @RequestParam(required = false) List<UUID> roleIds, HttpServletResponse response) {
        service.assignRoles(id, tenantId, roleIds);
        response.setHeader("HX-Trigger", "closeModal");
        return "admin/settings/users/row :: empty";
    }
}