package com.erp.core.security.internal.web.controller;

import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.internal.context.infrastructure.springsecurity.TenantContextRestorationFilter;
import com.erp.core.security.internal.tenant.event.TenantSwitchedEvent;
import com.erp.core.security.internal.tenant.port.TenantMembershipPort;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Session-scoped tenant switch.
 *
 * SECURITY:
 * - Requires an authenticated session (CSRF-protected browser chain).
 * - Every switch is verified against TenantMembershipPort.
 * - JWT callers CANNOT use this endpoint to forge a tenant swap: they
 *   authenticate via /api/**, which does not create a session, and the
 *   JWT chain ignores session attributes entirely.
 */
@RestController
@RequestMapping("/auth/tenant")
@PreAuthorize("isAuthenticated()")
public class TenantSwitchController {

    public record TenantOption(UUID tenantId, boolean active) {}

    private final CurrentActorProvider actorProvider;
    private final TenantMembershipPort membership;
    private final ApplicationEventPublisher events;

    public TenantSwitchController(CurrentActorProvider actorProvider,
                                  TenantMembershipPort membership,
                                  ApplicationEventPublisher events) {
        this.actorProvider = actorProvider;
        this.membership = membership;
        this.events = events;
    }

    @GetMapping("/list")
    public List<TenantOption> list() {
        CurrentActor actor = actorProvider.getCurrentActor();
        if (actor == null || !actor.isAuthenticated() || actor.id() == null) return List.of();
        return membership.findTenantsForActor(actor.id()).stream()
                .map(id -> new TenantOption(id, id.equals(actor.tenantId())))
                .toList();
    }

    @PostMapping("/switch/{tenantId}")
    public ResponseEntity<?> switchTo(@PathVariable UUID tenantId, HttpServletRequest request) {
        CurrentActor actor = actorProvider.getCurrentActor();
        if (actor == null || !actor.isAuthenticated() || actor.id() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthenticated"));
        }
        if (!membership.isMemberOf(actor.id(), tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "not a member of tenant"));
        }
        UUID previous = actor.tenantId();
        request.getSession(true)
                .setAttribute(TenantContextRestorationFilter.ACTIVE_TENANT_SESSION_ATTR, tenantId);
        events.publishEvent(new TenantSwitchedEvent(actor.id(), previous, tenantId, Instant.now()));
        return ResponseEntity.ok(Map.of(
                "activeTenantId", tenantId,
                "previousTenantId", previous
        ));
    }
}