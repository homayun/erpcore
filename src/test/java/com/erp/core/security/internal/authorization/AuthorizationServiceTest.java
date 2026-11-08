package com.erp.core.security.internal.authorization;

import com.erp.core.security.api.actor.ActorType;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.authorization.ActionDescriptor;
import com.erp.core.security.api.authorization.AuthorizationDecision;
import com.erp.core.security.api.authorization.AuthorizationRequest;
import com.erp.core.security.api.authorization.ResourceDescriptor;
import com.erp.core.security.internal.authorization.application.AttributeConditionEvaluator;
import com.erp.core.security.internal.authorization.application.AuthorizationService;
import com.erp.core.security.internal.authorization.application.FieldAuthorizationService;
import com.erp.core.security.internal.authorization.domain.AttributeCondition;
import com.erp.core.security.internal.authorization.domain.FieldRule;
import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.FieldRuleRepositoryPort;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServiceTest {

    private final UUID tenant = UUID.randomUUID();
    private final UUID otherTenant = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private static FieldRuleRepositoryPort emptyFieldRuleRepo() {
        return new FieldRuleRepositoryPort() {
            @Override public List<FieldRule> findByResourceType(String resourceType) { return List.of(); }
            @Override public List<FieldRule> findAll() { return List.of(); }
            @Override public FieldRule upsert(FieldRule rule) { return rule; }
            @Override public boolean delete(String resourceType, String fieldName) { return false; }
            @Override public boolean exists(String resourceType, String fieldName) { return false; }
        };
    }

    private AuthorizationService service(
            Set<PermissionKey> rolePermissions,
            List<AttributeCondition> conditions) {
        FieldAuthorizationService fieldService = new FieldAuthorizationService(
                emptyFieldRuleRepo(),
                (a, t) -> rolePermissions);
        return new AuthorizationService(
                new PermissionRepositoryPort() {
                    @Override public void upsertAll(java.util.Collection<PermissionDefinition> defs) {}
                    @Override public boolean exists(PermissionKey key) { return false; }
                    @Override public List<PermissionDefinition> findAll() { return List.of(); }
                },
                (a, t) -> rolePermissions,
                key -> conditions,
                new AttributeConditionEvaluator(),
                fieldService);
    }

    private CurrentActor actor(Set<String> authorities, UUID actorTenant) {
        return new CurrentActor(actorId, ActorType.USER, actorTenant, "alice", authorities);
    }

    private AuthorizationRequest request(CurrentActor actor, String verb, String scope) {
        return new AuthorizationRequest(actor,
                new ActionDescriptor(verb, scope),
                new ResourceDescriptor(scope, "ref-1", tenant),
                Map.of());
    }

    @Test
    void permitsWhenAuthorityMatches() {
        var svc = service(Set.of(), List.of());
        var decision = svc.authorize(request(actor(Set.of("FINANCE_INVOICE_APPROVE"), tenant),
                "APPROVE", "FINANCE_INVOICE"));
        assertThat(decision).isEqualTo(AuthorizationDecision.PERMIT);
    }

    @Test
    void deniesWhenAuthorityMissing() {
        var svc = service(Set.of(), List.of());
        var decision = svc.authorize(request(actor(Set.of("ROLE_USER"), tenant),
                "APPROVE", "FINANCE_INVOICE"));
        assertThat(decision).isEqualTo(AuthorizationDecision.DENY);
    }

    @Test
    void deniesCrossTenant() {
        var svc = service(Set.of(PermissionKey.of("FINANCE_INVOICE_APPROVE")), List.of());
        var decision = svc.authorize(request(actor(Set.of(), otherTenant),
                "APPROVE", "FINANCE_INVOICE"));
        assertThat(decision).isEqualTo(AuthorizationDecision.DENY);
    }

    @Test
    void deniesWhenAttributeConditionFails() {
        var condition = new AttributeCondition("amount", AttributeCondition.Operator.LTE, "10000");
        var svc = service(Set.of(), List.of(condition));

        var requestWithAmount = new AuthorizationRequest(
                actor(Set.of("FINANCE_INVOICE_APPROVE"), tenant),
                new ActionDescriptor("APPROVE", "FINANCE_INVOICE"),
                new ResourceDescriptor("FINANCE_INVOICE", "ref-1", tenant),
                Map.of("amount", 50000));

        assertThat(svc.authorize(requestWithAmount)).isEqualTo(AuthorizationDecision.DENY);
    }

    @Test
    void anonymousIsDenied() {
        var svc = service(Set.of(), List.of());
        var anon = new CurrentActor(null, ActorType.ANONYMOUS, null, null, Set.of());
        assertThat(svc.authorize(request(anon, "APPROVE", "FINANCE_INVOICE")))
                .isEqualTo(AuthorizationDecision.DENY);
    }

    @Test
    void roleBasedPermissionsAreConsidered() {
        var svc = service(Set.of(PermissionKey.of("FINANCE_INVOICE_APPROVE")), List.of());
        var decision = svc.authorize(request(actor(Set.of(), tenant),
                "APPROVE", "FINANCE_INVOICE"));
        assertThat(decision).isEqualTo(AuthorizationDecision.PERMIT);
    }
}