package com.erp.core.security.internal.authorization.adapter.in.springsecurity;

import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.api.authorization.ActionDescriptor;
import com.erp.core.security.api.authorization.AuthorizationDecision;
import com.erp.core.security.api.authorization.AuthorizationPort;
import com.erp.core.security.api.authorization.AuthorizationRequest;
import com.erp.core.security.api.authorization.ResourceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class ErpPermissionEvaluator implements PermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ErpPermissionEvaluator.class);

    private final AuthorizationPort authorizationPort;
    private final CurrentActorProvider actorProvider;

    public ErpPermissionEvaluator(AuthorizationPort authorizationPort,
                                  CurrentActorProvider actorProvider) {
        this.authorizationPort = authorizationPort;
        this.actorProvider = actorProvider;
    }

    @Override
    public boolean hasPermission(Authentication authentication,
                                 Object targetDomainObject, Object permission) {
        return doCheck(String.valueOf(targetDomainObject), "UNKNOWN", String.valueOf(permission));
    }

    @Override
    public boolean hasPermission(Authentication authentication,
                                 Serializable targetId, String targetType, Object permission) {
        return doCheck(String.valueOf(targetId), targetType, String.valueOf(permission));
    }

    private boolean doCheck(String externalRef, String resourceType, String permission) {
        CurrentActor actor = actorProvider.getCurrentActor();
        if (log.isDebugEnabled()) {
            log.debug("EVAL resource={} externalRef={} permission={} actor={} tenant={} type={}",
                    resourceType, externalRef, permission,
                    actor == null ? null : actor.id(),
                    actor == null ? null : actor.tenantId(),
                    actor == null ? null : actor.type());
        }
        if (actor == null || !actor.isAuthenticated()) return false;
        UUID tenantId = actor.tenantId();
        if (tenantId == null) return false;

        AuthorizationRequest req = new AuthorizationRequest(
                actor,
                new ActionDescriptor(permission, resourceType),
                new ResourceDescriptor(resourceType, externalRef, tenantId),
                Map.of());

        AuthorizationDecision decision = authorizationPort.authorize(req);
        if (log.isDebugEnabled()) {
            log.debug("EVAL decision={} for action={}_{}", decision, resourceType, permission);
        }
        return decision == AuthorizationDecision.PERMIT;
    }
}