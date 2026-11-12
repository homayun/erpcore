package com.erp.core.admin.web;

import com.erp.core.admin.internal.company.adapter.out.jpa.MasterCompanyRepository;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Session-scoped division filter (MasterCompany).
 *
 * This is NOT a security boundary. Tenant isolation is enforced by
 * TenantContextHolder + role-based permissions. This controller only
 * narrows the UI's data listing to one master_company within the
 * caller's own tenant.
 *
 * SECURITY:
 * - CSRF-protected (browser filter chain).
 * - Cross-tenant division IDs are rejected.
 * - Session tampering is benign (filtered to caller's tenant on read).
 */
@RestController
@RequestMapping("/admin/division")
@PreAuthorize("isAuthenticated()")
public class DivisionSwitchController {

    public static final String DIVISION_ATTR = "erp.activeDivisionId";

    private final CurrentActorProvider actorProvider;
    private final MasterCompanyRepository masterCompanyRepo;

    public DivisionSwitchController(CurrentActorProvider actorProvider,
                                    MasterCompanyRepository masterCompanyRepo) {
        this.actorProvider = actorProvider;
        this.masterCompanyRepo = masterCompanyRepo;
    }

    @PostMapping("/switch/{divisionId}")
    public ResponseEntity<?> switchDivision(@PathVariable UUID divisionId,
                                            HttpServletRequest request) {
        CurrentActor actor = actorProvider.getCurrentActor();
        if (actor == null || actor.tenantId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean belongsToTenant = masterCompanyRepo.findByTenantId(actor.tenantId()).stream()
                .anyMatch(e -> e.getId().equals(divisionId));
        if (!belongsToTenant) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "division not in tenant"));
        }
        request.getSession(true).setAttribute(DIVISION_ATTR, divisionId);
        return ResponseEntity.ok(Map.of("activeDivisionId", divisionId));
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearDivision(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.removeAttribute(DIVISION_ATTR);
        return ResponseEntity.ok(Map.of("activeDivisionId", "ALL"));
    }
}