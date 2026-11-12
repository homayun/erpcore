package com.erp.core.admin.web;

import com.erp.core.admin.tenant.application.TenantService;
import com.erp.core.admin.web.dto.CreateTenantRequest;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/tenants")
@PreAuthorize("hasAuthority('TENANT_MANAGE')")
public class TenantWebController {

    private final TenantService service;
    private final CurrentActorProvider actorProvider;

    public TenantWebController(TenantService service, CurrentActorProvider actorProvider) {
        this.service = service;
        this.actorProvider = actorProvider;
    }

    @GetMapping
    public String list(HttpServletRequest request, Model model) {
        model.addAttribute("tenants", service.list());
        if (request.getHeader("HX-Request") != null) return "admin/settings/tenants/index :: content";
        return "admin/settings/tenants/index";
    }

    @GetMapping("/new")
    public String newForm() {
        return "admin/settings/tenants/form :: form-content";
    }

    @PostMapping
    public String create(@RequestParam String code,
                         @RequestParam String name,
                         @RequestParam(required = false) String subscriptionPlan,
                         @RequestParam(required = false) String initialAdminUsername,
                         @RequestParam(required = false) String initialAdminPassword,
                         Model model, HttpServletResponse response) {
        CurrentActor actor = actorProvider.getCurrentActor();
        if (actor == null || actor.id() == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return "admin/settings/tenants/row :: empty";
        }
        service.create(new CreateTenantRequest(code, name, subscriptionPlan,
                initialAdminUsername, initialAdminPassword), actor.id());
        response.setHeader("HX-Trigger", "closeModal");
        model.addAttribute("tenants", service.list());
        return "admin/settings/tenants/index :: content";
    }

    @PostMapping("/{id}/suspend")
    public String suspend(@PathVariable UUID id, Model model) {
        service.updateStatus(id, "SUSPENDED");
        model.addAttribute("tenants", service.list());
        return "admin/settings/tenants/index :: content";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable UUID id, Model model) {
        service.updateStatus(id, "ACTIVE");
        model.addAttribute("tenants", service.list());
        return "admin/settings/tenants/index :: content";
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<?> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}