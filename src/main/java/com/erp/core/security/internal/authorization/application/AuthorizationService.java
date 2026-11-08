package com.erp.core.security.internal.authorization.application;

import com.erp.core.security.api.actor.ActorType;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.authorization.AuthorizationDecision;
import com.erp.core.security.api.authorization.AuthorizationPort;
import com.erp.core.security.api.authorization.AuthorizationRequest;
import com.erp.core.security.api.authorization.FieldAccessDecision;
import com.erp.core.security.api.authorization.FieldAccessRequest;
import com.erp.core.security.internal.authorization.domain.AttributeCondition;
import com.erp.core.security.internal.authorization.domain.AuthorizationDecisionRecord;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.AttributeConditionRepositoryPort;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import com.erp.core.security.internal.authorization.port.RoleRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AuthorizationService implements AuthorizationPort {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    private final PermissionRepositoryPort permissionRepository;
    private final RoleRepositoryPort roleRepository;
    private final AttributeConditionRepositoryPort conditionRepository;
    private final AttributeConditionEvaluator conditionEvaluator;
    private final FieldAuthorizationService fieldService;

    public AuthorizationService(PermissionRepositoryPort permissionRepository,
                                RoleRepositoryPort roleRepository,
                                AttributeConditionRepositoryPort conditionRepository,
                                AttributeConditionEvaluator conditionEvaluator,
                                FieldAuthorizationService fieldService) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.conditionRepository = conditionRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.fieldService = fieldService;
    }

    @Override
    public AuthorizationDecision authorize(AuthorizationRequest request) {
        return evaluate(request).decision();
    }

    @Override
    public FieldAccessDecision authorizeFieldAccess(FieldAccessRequest request) {
        if (request == null || request.actor() == null) return FieldAccessDecision.denyAll();
        if (request.actor().type() == ActorType.ANONYMOUS) return FieldAccessDecision.denyAll();
        if (!Objects.equals(request.actor().tenantId(), request.resource().tenantId())) {
            return FieldAccessDecision.denyAll();
        }
        return fieldService.decide(request, effectivePermissions(request.actor()));
    }

    private AuthorizationDecisionRecord evaluate(AuthorizationRequest request) {
        CurrentActor actor = request.actor();
        if (actor == null || actor.type() == ActorType.ANONYMOUS) {
            log.debug("AUTHZ deny: anonymous actor");
            return AuthorizationDecisionRecord.deny("anonymous actor");
        }
        if (!Objects.equals(actor.tenantId(), request.resource().tenantId())) {
            log.debug("AUTHZ deny: cross-tenant actorTenant={} resourceTenant={}",
                    actor.tenantId(), request.resource().tenantId());
            return AuthorizationDecisionRecord.deny("cross-tenant resource access");
        }
        PermissionKey required = PermissionKey.of(request.action().toCanonicalString());
        Set<PermissionKey> effective = effectivePermissions(actor);
        if (log.isDebugEnabled()) {
            log.debug("AUTHZ check actor={} required={} effective={}",
                    actor.id(), required, effective);
        }
        if (!effective.contains(required)) {
            log.debug("AUTHZ deny: missing {}", required);
            return AuthorizationDecisionRecord.deny("missing permission " + required);
        }
        List<AttributeCondition> conditions = conditionRepository.findForPermission(required);
        for (AttributeCondition c : conditions) {
            if (!conditionEvaluator.evaluate(c, request.attributes())) {
                log.debug("AUTHZ deny: attribute condition failed for {}", required);
                return AuthorizationDecisionRecord.deny("attribute condition failed for " + required);
            }
        }
        return AuthorizationDecisionRecord.permit("granted " + required);
    }

    private Set<PermissionKey> effectivePermissions(CurrentActor actor) {
        Set<PermissionKey> result = new HashSet<>();
        if (actor.authorities() != null) {
            for (String a : actor.authorities()) {
                try { result.add(PermissionKey.of(a)); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        try {
            result.addAll(roleRepository.resolvePermissions(actor.id(), actor.tenantId()));
        } catch (RuntimeException ex) {
            log.warn("Failed to resolve role permissions for actor={}: {}",
                    actor.id(), ex.getMessage(), ex);
        }
        return result;
    }
}