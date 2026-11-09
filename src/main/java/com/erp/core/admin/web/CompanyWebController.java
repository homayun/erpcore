package com.erp.core.admin.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/companies")
public class CompanyWebController {

    // মেইন পেজ (পুরো HTML লোড করবে)
    @GetMapping
    public String companiesPage() {
        return "admin/companies/index";
    }

    // HTMX এর জন্য ফ্র্যাগমেন্ট (শুধু ফর্মটি লোড করবে)
    @GetMapping("/new")
    public String newCompanyForm() {
        return "admin/companies/form :: form-content";
    }
}