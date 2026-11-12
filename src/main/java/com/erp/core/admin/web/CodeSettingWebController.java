package com.erp.core.admin.web;
import com.erp.core.admin.internal.setting.application.CodeSettingService;
import com.erp.core.admin.internal.setting.domain.CodeSetting;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Controller
@RequestMapping("/admin/settings/code-settings")
public class CodeSettingWebController {
    private final CodeSettingService service;
    private final CurrentActorProvider actorProvider;

    public CodeSettingWebController(CodeSettingService service, CurrentActorProvider actorProvider) {
        this.service = service;
        this.actorProvider = actorProvider;
    }

    /**
     * Resolves the caller's active tenant from the authenticated actor.
     * Fails closed: anonymous or tenant-less actors MUST NOT reach data.
     */
    private UUID tenantId() {
        CurrentActor a = actorProvider.getCurrentActor();
        if (a == null || !a.isAuthenticated() || a.tenantId() == null) {
            throw new IllegalStateException("No authenticated tenant context");
        }
        return a.tenantId();
    }

    @GetMapping
    public String list(HttpServletRequest request, Model model, @RequestParam(defaultValue = "all") String filter) {
        model.addAttribute("settings", service.getAll(tenantId(), filter));
        model.addAttribute("filter", filter);
        if (request.getHeader("HX-Request") != null) return "admin/settings/code-settings/index :: content";
        return "admin/settings/code-settings/index";
    }
    @GetMapping("/new") public String newForm() { return "admin/settings/code-settings/form :: form-content"; }
    @GetMapping("/{id}/edit") public String editForm(@PathVariable UUID id, Model model) { model.addAttribute("c", service.getById(id)); return "admin/settings/code-settings/form :: form-content"; }
    @PostMapping public String create(@RequestParam String category, @RequestParam String code, @RequestParam String name, Model model) { CodeSetting c = service.create(tenantId(), category, code, name); model.addAttribute("c", c); return "admin/settings/code-settings/row :: row"; }
    @PostMapping("/{id}") public String update(@PathVariable UUID id, @RequestParam String category, @RequestParam String code, @RequestParam String name, Model model) { CodeSetting c = service.update(id, category, code, name); model.addAttribute("c", c); return "admin/settings/code-settings/row :: row"; }
    @PostMapping("/{id}/soft-delete") public String softDelete(@PathVariable UUID id, Model model) { service.softDelete(id); model.addAttribute("c", service.getById(id)); return "admin/settings/code-settings/row :: row"; }
    @PostMapping("/{id}/restore") public String restore(@PathVariable UUID id, Model model) { service.restore(id); model.addAttribute("c", service.getById(id)); return "admin/settings/code-settings/row :: row"; }
    @DeleteMapping("/{id}") public String hardDelete(@PathVariable UUID id) { service.hardDelete(id); return "admin/settings/code-settings/row :: empty"; }
}