package com.erp.core.security.internal.authorization;

import com.erp.core.security.api.actor.ActorType;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.authorization.ActionDescriptor;
import com.erp.core.security.api.authorization.AuthorizationDecision;
import com.erp.core.security.api.authorization.AuthorizationPort;
import com.erp.core.security.api.authorization.AuthorizationRequest;
import com.erp.core.security.api.authorization.ResourceDescriptor;
import com.erp.core.security.internal.authorization.application.AuthenticationFreshnessEvaluator;
import com.erp.core.security.internal.authorization.application.SensitiveOperationGuard;
import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import org.junit.jupiter.api.Test;

import com.erp.core.security.api.authorization.FieldAccessDecision;
import com.erp.core.security.api.authorization.FieldAccessRequest;


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveOperationGuardTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final UUID TENANT = UUID.randomUUID();

    private final CurrentActor actor =
            new CurrentActor(UUID.randomUUID(), ActorType.USER, TENANT, "alice", Set.of());
    private final AuthorizationRequest request = new AuthorizationRequest(
            actor,
            new ActionDescriptor("APPROVE", "FINANCE_INVOICE"),
            new ResourceDescriptor("FINANCE_INVOICE", "ref-1", TENANT),
            Map.of());

    private SensitiveOperationGuard guard(AuthorizationDecision baseDecision, boolean sensitive) {
    	AuthorizationPort stubAuthPort = new AuthorizationPort() {
    	    @Override
    	    public AuthorizationDecision authorize(AuthorizationRequest req) {
    	        return baseDecision;
    	    }

    	    @Override
    	    public FieldAccessDecision authorizeFieldAccess(FieldAccessRequest req) {
    	        // Not exercised in these tests — return a safe default.
    	        return FieldAccessDecision.denyAll();
    	    }
    	};
        PermissionRepositoryPort stubRepo = new PermissionRepositoryPort() {
            @Override public void upsertAll(java.util.Collection<PermissionDefinition> d) { }
            @Override public boolean exists(PermissionKey key) { return true; }
            @Override public List<PermissionDefinition> findAll() {
                return List.of(new PermissionDefinition(
                        PermissionKey.of("FINANCE_INVOICE_APPROVE"),
                        "FINANCE", "Approve invoices", sensitive));
            }
        };
        AuthenticationFreshnessEvaluator evaluator = new AuthenticationFreshnessEvaluator(
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(15));
        return new SensitiveOperationGuard(stubAuthPort, stubRepo, evaluator);
    }

    @Test
    void nonSensitivePermitStaysPermit() {
        var result = guard(AuthorizationDecision.PERMIT, false)
                .authorize(request, NOW.minus(Duration.ofHours(10))); // stale auth irrelevant
        assertThat(result).isEqualTo(AuthorizationDecision.PERMIT);
    }

    @Test
    void sensitivePermitWithFreshAuthStaysPermit() {
        var result = guard(AuthorizationDecision.PERMIT, true)
                .authorize(request, NOW.minus(Duration.ofMinutes(5)));
        assertThat(result).isEqualTo(AuthorizationDecision.PERMIT);
    }

    @Test
    void sensitivePermitWithStaleAuthBecomesStepUp() {
        var result = guard(AuthorizationDecision.PERMIT, true)
                .authorize(request, NOW.minus(Duration.ofMinutes(30)));
        assertThat(result).isEqualTo(AuthorizationDecision.STEP_UP_REQUIRED);
    }

    @Test
    void sensitivePermitWithNullAuthBecomesStepUp() {
        var result = guard(AuthorizationDecision.PERMIT, true)
                .authorize(request, null);
        assertThat(result).isEqualTo(AuthorizationDecision.STEP_UP_REQUIRED);
    }

    @Test
    void denyStaysDenyRegardlessOfAuthAge() {
        var result = guard(AuthorizationDecision.DENY, true)
                .authorize(request, NOW); // fresh auth cannot override DENY
        assertThat(result).isEqualTo(AuthorizationDecision.DENY);
    }

    @Test
    void baseStepUpPropagates() {
        var result = guard(AuthorizationDecision.STEP_UP_REQUIRED, false)
                .authorize(request, NOW);
        assertThat(result).isEqualTo(AuthorizationDecision.STEP_UP_REQUIRED);
    }

    @Test
    void nullRequestDenies() {
        var result = guard(AuthorizationDecision.PERMIT, true)
                .authorize(null, NOW);
        assertThat(result).isEqualTo(AuthorizationDecision.DENY);
    }
}