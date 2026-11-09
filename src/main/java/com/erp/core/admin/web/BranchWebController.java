package com.erp.core.admin.web;

import com.erp.core.admin.internal.company.application.BranchService;
import com.erp.core.admin.internal.company.domain.Branch;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/branches")
public class BranchWebController {
    private final BranchService service;
    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public BranchWebController(BranchService service) { this.service = service; }

    @GetMapping
    public String list(HttpServletRequest request, Model model, @RequestParam(defaultValue = "all") String filter) {
        model.addAttribute("branches", service.getBranches(tenantId, filter));
        model.addAttribute("filter", filter);
        if (request.getHeader("HX-Request") != null) return "admin/settings/branches/index :: content";
        return "admin/settings/branches/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("companies", service.getCompanies(tenantId));
        return "admin/settings/branches/form :: form-content";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("branch", service.getById(id));
        model.addAttribute("companies", service.getCompanies(tenantId));
        return "admin/settings/branches/form :: form-content";
    }

    @PostMapping
    public String create(@RequestParam UUID companyId, @RequestParam String name, @RequestParam String code, Model model) {
        Branch b = service.createBranch(tenantId, companyId, name, code);
        model.addAttribute("branch", b);
        return "admin/settings/branches/row :: row";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id, @RequestParam UUID companyId, @RequestParam String name, @RequestParam String code, Model model) {
        Branch b = service.updateBranch(id, companyId, name, code);
        model.addAttribute("branch", b);
        return "admin/settings/branches/row :: row";
    }

    @PostMapping("/{id}/soft-delete")
    public String softDelete(@PathVariable UUID id, Model model) {
        service.softDeleteBranch(id);
        model.addAttribute("branch", service.getById(id));
        return "admin/settings/branches/row :: row";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable UUID id, Model model) {
        service.restoreBranch(id);
        model.addAttribute("branch", service.getById(id));
        return "admin/settings/branches/row :: row";
    }

    @DeleteMapping("/{id}")
    public String hardDelete(@PathVariable UUID id) {
        service.hardDeleteBranch(id);
        return "admin/settings/branches/row :: empty";
    }
}