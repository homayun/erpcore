package com.erp.core.admin.web;

import com.erp.core.admin.internal.company.application.MasterCompanyService;
import com.erp.core.admin.internal.company.domain.MasterCompany;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/master-companies")
public class MasterCompanyWebController {

    private final MasterCompanyService service;
    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public MasterCompanyWebController(MasterCompanyService service) {
        this.service = service;
    }

    @GetMapping
    public String list(HttpServletRequest request, Model model, @RequestParam(defaultValue = "all") String filter) {
        model.addAttribute("masterCompanies", service.getAll(tenantId, filter));
        model.addAttribute("filter", filter);
        if (request.getHeader("HX-Request") != null) return "admin/settings/master-companies/index :: content";
        return "admin/settings/master-companies/index";
    }

    @GetMapping("/new")
    public String newForm() {
        return "admin/settings/master-companies/form :: form-content";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("mc", service.getById(id));
        return "admin/settings/master-companies/form :: form-content";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam String code, Model model) {
        MasterCompany mc = service.create(tenantId, name, code);
        model.addAttribute("mc", mc);
        return "admin/settings/master-companies/row :: row";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id, @RequestParam String name, @RequestParam String code, Model model) {
        MasterCompany mc = service.update(id, name, code);
        model.addAttribute("mc", mc);
        return "admin/settings/master-companies/row :: row";
    }

    @PostMapping("/{id}/soft-delete")
    public String softDelete(@PathVariable UUID id, Model model) {
        service.softDelete(id);
        model.addAttribute("mc", service.getById(id));
        return "admin/settings/master-companies/row :: row";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable UUID id, Model model) {
        service.restore(id);
        model.addAttribute("mc", service.getById(id));
        return "admin/settings/master-companies/row :: row";
    }

    @DeleteMapping("/{id}")
    public String hardDelete(@PathVariable UUID id) {
        service.hardDelete(id);
        return "admin/settings/master-companies/row :: empty";
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=master_companies.csv");
        service.exportCsv(tenantId, response.getWriter());
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file, HttpServletRequest request, Model model) {
        service.importCsv(tenantId, file);
        model.addAttribute("masterCompanies", service.getAll(tenantId, "all"));
        model.addAttribute("filter", "all");
        return "admin/settings/master-companies/index :: content";
    }

    // এটি HTMX-কে 400 Bad Request পাঠাবে, যাতে layout.js এর alert কাজ করে
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}