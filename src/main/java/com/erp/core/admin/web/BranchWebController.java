package com.erp.core.admin.web;

import com.erp.core.admin.internal.company.application.BranchService;
import com.erp.core.admin.internal.company.domain.Branch;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/branches")
public class BranchWebController {
    private final BranchService service;
    private final CurrentActorProvider actorProvider;

    public BranchWebController(BranchService service, CurrentActorProvider actorProvider) {
        this.service = service;
        this.actorProvider = actorProvider;
    }

    private UUID tenantId() {
        CurrentActor a = actorProvider.getCurrentActor();
        if (a == null || a.tenantId() == null) throw new IllegalStateException("No tenant context");
        return a.tenantId();
    }

    private static UUID activeDivisionId(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s == null) return null;
        Object v = s.getAttribute(DivisionSwitchController.DIVISION_ATTR);
        return v instanceof UUID u ? u : null;
    }

    @GetMapping
    public String list(HttpServletRequest request, Model model, @RequestParam(defaultValue = "all") String filter) {
        UUID div = activeDivisionId(request);
        var branches = div != null
                ? service.getBranchesByDivision(tenantId(), div, filter)
                : service.getBranches(tenantId(), filter);
        model.addAttribute("branches", branches);
        model.addAttribute("filter", filter);
        if (request.getHeader("HX-Request") != null) return "admin/settings/branches/index :: content";
        return "admin/settings/branches/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("companies", service.getCompanies(tenantId()));
        return "admin/settings/branches/form :: form-content";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("branch", service.getById(id));
        model.addAttribute("companies", service.getCompanies(tenantId()));
        return "admin/settings/branches/form :: form-content";
    }

    @PostMapping
    public String create(@RequestParam UUID companyId, @RequestParam String name,
                         @RequestParam String code, Model model) {
        Branch b = service.createBranch(tenantId(), companyId, name, code);
        model.addAttribute("b", b);
        return "admin/settings/branches/row :: row";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id, @RequestParam UUID companyId,
                         @RequestParam String name, @RequestParam String code, Model model) {
        Branch b = service.updateBranch(id, companyId, name, code);
        model.addAttribute("b", b);
        return "admin/settings/branches/row :: row";
    }

    @PostMapping("/{id}/soft-delete")
    public String softDelete(@PathVariable UUID id, Model model) {
        service.softDeleteBranch(id);
        model.addAttribute("b", service.getById(id));
        return "admin/settings/branches/row :: row";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable UUID id, Model model) {
        service.restoreBranch(id);
        model.addAttribute("b", service.getById(id));
        return "admin/settings/branches/row :: row";
    }

    @DeleteMapping("/{id}")
    public String hardDelete(@PathVariable UUID id) {
        service.hardDeleteBranch(id);
        return "admin/settings/branches/row :: empty";
    }
}