package com.erp.core.admin.web;

import com.erp.core.admin.internal.company.adapter.out.jpa.MasterCompanyEntity;
import com.erp.core.admin.internal.company.adapter.out.jpa.MasterCompanyRepository;
import com.erp.core.admin.internal.company.domain.MasterCompany;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.UUID;

@ControllerAdvice
public class DivisionModelAdvice {

    private final CurrentActorProvider actorProvider;
    private final MasterCompanyRepository masterCompanyRepo;

    public DivisionModelAdvice(CurrentActorProvider actorProvider,
                                MasterCompanyRepository masterCompanyRepo) {
        this.actorProvider = actorProvider;
        this.masterCompanyRepo = masterCompanyRepo;
    }

    @ModelAttribute
    public void addDivisionModel(Model model, HttpServletRequest request) {
        CurrentActor actor = actorProvider.getCurrentActor();
        if (actor == null || !actor.isAuthenticated() || actor.tenantId() == null) return;

        List<MasterCompany> divisions = masterCompanyRepo.findByTenantId(actor.tenantId()).stream()
                .filter(MasterCompanyEntity::isActive)
                .map(MasterCompanyEntity::toDomain)
                .toList();

        UUID activeDivisionId = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object v = session.getAttribute(DivisionSwitchController.DIVISION_ATTR);
            if (v instanceof UUID u) activeDivisionId = u;
        }

        model.addAttribute("availableDivisions", divisions);
        model.addAttribute("activeDivisionId", activeDivisionId);
        model.addAttribute("hasMultipleDivisions", divisions.size() > 1);
    }
}