package com.erp.core.admin.internal.web;

import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminWebController {

    private final CurrentActorProvider actorProvider;

    public AdminWebController(CurrentActorProvider actorProvider) {
        this.actorProvider = actorProvider;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        CurrentActor actor = actorProvider.getCurrentActor();
        model.addAttribute("username", actor.principal());
        model.addAttribute("tenantId", actor.tenantId().toString());
        return "admin/dashboard";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        CurrentActor actor = actorProvider.getCurrentActor();
        model.addAttribute("tenantId", actor.tenantId().toString());
        return "admin/settings";
    }
}