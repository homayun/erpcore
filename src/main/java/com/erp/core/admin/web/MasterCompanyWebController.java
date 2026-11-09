package com.erp.core.admin.web;

import com.erp.core.admin.internal.company.application.CompanyService;
import com.erp.core.admin.internal.company.domain.MasterCompany;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/master-companies")
public class MasterCompanyWebController {

    private final CompanyService companyService;
    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public MasterCompanyWebController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public String masterCompaniesPage(HttpServletRequest request, Model model) {
        model.addAttribute("masterCompanies", companyService.getMasterCompanies(tenantId));
        if (request.getHeader("HX-Request") != null) return "admin/settings/master-companies/index :: content";
        return "admin/settings/master-companies/index";
    }

    @GetMapping("/new")
    public String newMasterCompanyForm() {
        return "admin/settings/master-companies/form :: form-content";
    }

    @PostMapping
    public String saveMasterCompany(@RequestParam String name, 
                                    @RequestParam String code, 
                                    Model model) {
        MasterCompany mc = companyService.createMasterCompany(tenantId, name, code);
        model.addAttribute("mc", mc);
        return "admin/settings/master-companies/row :: master-company-row";
    }

    @DeleteMapping("/{id}")
    public String deleteMasterCompany(@PathVariable UUID id) {
        companyService.deleteMasterCompany(id);
        return "admin/settings/master-companies/row :: empty";
    }
}