package com.erp.core.admin.web;

import com.erp.core.admin.internal.company.application.CompanyService;
import com.erp.core.admin.internal.company.domain.Company;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/companies")
public class CompanyWebController {
    private final CompanyService service;
    private final CurrentActorProvider actorProvider;

    public CompanyWebController(CompanyService service, CurrentActorProvider actorProvider) {
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
        var companies = div != null
                ? service.getCompaniesByDivision(tenantId(), div, filter)
                : service.getCompanies(tenantId(), filter);
        model.addAttribute("companies", companies);
        model.addAttribute("filter", filter);
        if (request.getHeader("HX-Request") != null) return "admin/settings/companies/index :: content";
        return "admin/settings/companies/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("masterCompanies", service.getMasterCompanies(tenantId()));
        return "admin/settings/companies/form :: form-content";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("company", service.getById(id));
        model.addAttribute("masterCompanies", service.getMasterCompanies(tenantId()));
        return "admin/settings/companies/form :: form-content";
    }

    @PostMapping
    public String create(@RequestParam UUID masterCompanyId, @RequestParam String name,
                         @RequestParam String regNumber, @RequestParam(required = false) String taxId,
                         Model model) {
        Company c = service.createCompany(tenantId(), masterCompanyId, name, regNumber, taxId);
        model.addAttribute("c", c);
        return "admin/settings/companies/row :: row";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id, @RequestParam UUID masterCompanyId, @RequestParam String name,
                         @RequestParam String regNumber, @RequestParam(required = false) String taxId,
                         Model model) {
        Company c = service.updateCompany(id, masterCompanyId, name, regNumber, taxId);
        model.addAttribute("c", c);
        return "admin/settings/companies/row :: row";
    }

    @PostMapping("/{id}/soft-delete")
    public String softDelete(@PathVariable UUID id, Model model) {
        service.softDeleteCompany(id);
        model.addAttribute("c", service.getById(id));
        return "admin/settings/companies/row :: row";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable UUID id, Model model) {
        service.restoreCompany(id);
        model.addAttribute("c", service.getById(id));
        return "admin/settings/companies/row :: row";
    }

    @DeleteMapping("/{id}")
    public String hardDelete(@PathVariable UUID id) {
        service.hardDeleteCompany(id);
        return "admin/settings/companies/row :: empty";
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}