package com.erp.core.admin.web;

import com.erp.core.admin.application.AdminPermissionService;
import com.erp.core.security.internal.authorization.adapter.out.jpa.PermissionEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/settings/permissions")
public class PermissionWebController {

    private final AdminPermissionService service;

    public PermissionWebController(AdminPermissionService service) {
        this.service = service;
    }

    @GetMapping
    public String list(HttpServletRequest request, Model model,
                       @RequestParam(defaultValue = "all") String filter) {
        model.addAttribute("permissions", service.list(filter));
        model.addAttribute("filter", filter);
        if (request.getHeader("HX-Request") != null) return "admin/settings/permissions/index :: content";
        return "admin/settings/permissions/index";
    }

    @GetMapping("/new")
    public String newForm() {
        return "admin/settings/permissions/form :: form-content";
    }

    @GetMapping("/{key}/edit")
    public String editForm(@PathVariable String key, Model model) {
        model.addAttribute("p", service.getById(key));
        return "admin/settings/permissions/form :: form-content";
    }

    @PostMapping
    public String create(@RequestParam String key,
                         @RequestParam String group,
                         @RequestParam String description,
                         @RequestParam(defaultValue = "false") boolean sensitive,
                         Model model) {
        PermissionEntity p = service.create(key, group, description, sensitive);
        model.addAttribute("p", p);
        return "admin/settings/permissions/row :: row";
    }

    @PostMapping("/{key}")
    public String update(@PathVariable String key,
                         @RequestParam String group,
                         @RequestParam String description,
                         @RequestParam(defaultValue = "false") boolean sensitive,
                         Model model) {
        PermissionEntity p = service.update(key, group, description, sensitive);
        model.addAttribute("p", p);
        return "admin/settings/permissions/row :: row";
    }

    @PostMapping("/{key}/soft-delete")
    public String softDelete(@PathVariable String key, Model model) {
        service.softDelete(key);
        model.addAttribute("p", service.getById(key));
        return "admin/settings/permissions/row :: row";
    }

    @PostMapping("/{key}/restore")
    public String restore(@PathVariable String key, Model model) {
        service.restore(key);
        model.addAttribute("p", service.getById(key));
        return "admin/settings/permissions/row :: row";
    }

    @DeleteMapping("/{key}")
    public String hardDelete(@PathVariable String key, HttpServletResponse response) {
        try {
            service.hardDelete(key);
        } catch (Exception ex) {
            response.setHeader("HX-Trigger", "showError");
            throw new IllegalStateException("Cannot delete: permission is referenced by roles or field rules.");
        }
        return "admin/settings/permissions/row :: empty";
    }
}