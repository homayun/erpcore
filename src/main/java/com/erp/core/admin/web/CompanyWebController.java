package com.erp.core.admin.web;

import com.erp.core.admin.internal.company.application.CompanyService;
import com.erp.core.admin.internal.company.domain.Company;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/companies")
public class CompanyWebController {

    private final CompanyService companyService;
    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public CompanyWebController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public String companiesPage(HttpServletRequest request, Model model) {
        model.addAttribute("companies", companyService.getCompanies(tenantId));
        if (request.getHeader("HX-Request") != null) return "admin/settings/companies/index :: content";
        return "admin/settings/companies/index";
    }

    @GetMapping("/new")
    public String newCompanyForm() {
        return "admin/settings/companies/form :: form-content";
    }

    @PostMapping
    public String saveCompany(@RequestParam String name, 
                              @RequestParam String regNumber, 
                              @RequestParam(required = false) String taxId, 
                              Model model) {
        Company company = companyService.createCompany(tenantId, name, regNumber, taxId);
        model.addAttribute("company", company);
        return "admin/settings/companies/row :: company-row";
    }

    @DeleteMapping("/{id}")
    public String deleteCompany(@PathVariable UUID id) {
        companyService.deleteCompany(id);
        return "admin/settings/companies/row :: empty";
    }
}