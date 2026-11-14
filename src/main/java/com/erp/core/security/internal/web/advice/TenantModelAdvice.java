package com.erp.core.security.internal.web.advice;

import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.internal.tenant.port.TenantMembershipPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.UUID;

@ControllerAdvice
public class TenantModelAdvice {

    public  static final String CACHE_ATTR = "erp.tenant.membership.cache";

    private final CurrentActorProvider actorProvider;
    private final TenantMembershipPort membership;

    public TenantModelAdvice(CurrentActorProvider actorProvider, TenantMembershipPort membership) {
        this.actorProvider = actorProvider;
        this.membership = membership;
    }

    @ModelAttribute
    public void addTenantModel(Model model, HttpServletRequest request) {
        CurrentActor actor = actorProvider.getCurrentActor();
        if (actor == null || !actor.isAuthenticated() || actor.id() == null) return;

        HttpSession session = request.getSession(false);
        List<UUID> tenants;
        if (session != null) {
            @SuppressWarnings("unchecked")
            List<UUID> cached = (List<UUID>) session.getAttribute(CACHE_ATTR);
            if (cached == null) {
                cached = membership.findTenantsForActor(actor.id()).stream().sorted().toList();
                session.setAttribute(CACHE_ATTR, cached);
            }
            tenants = cached;
        } else {
            tenants = membership.findTenantsForActor(actor.id()).stream().sorted().toList();
        }

        model.addAttribute("activeTenantId", actor.tenantId());
        model.addAttribute("availableTenants", tenants);
        model.addAttribute("hasMultipleTenants", tenants.size() > 1);
    }
}