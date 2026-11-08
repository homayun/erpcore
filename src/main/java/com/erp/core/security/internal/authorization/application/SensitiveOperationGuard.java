package com.erp.core.security.internal.authorization.application;

import com.erp.core.security.api.authorization.AuthorizationDecision;
import com.erp.core.security.api.authorization.AuthorizationPort;
import com.erp.core.security.api.authorization.AuthorizationRequest;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Wraps {@link AuthorizationPort} to enforce step-up authentication on
 * permissions marked {@code sensitive = true} in the catalog.
 *
 * Decision matrix:
 *
 *  Base decision   | Sensitive? | Auth fresh? | Final decision
 *  ----------------+------------+-------------+----------------
 *  PERMIT          | no         | n/a         | PERMIT
 *  PERMIT          | yes        | yes         | PERMIT
 *  PERMIT          | yes        | no / null   | STEP_UP_REQUIRED
 *  DENY            | any        | any         | DENY
 *  STEP_UP (base)  | any        | any         | STEP_UP_REQUIRED
 *
 * Notes:
 * - Step-up NEVER upgrades DENY to PERMIT. Fresh auth cannot bypass a
 *   missing permission.
 * - The caller supplies the authenticatedAt timestamp; this class does not
 *   read SecurityContext directly (keeps hexagonal purity).
 */
public class SensitiveOperationGuard {

    private static final Logger log = LoggerFactory.getLogger(SensitiveOperationGuard.class);

    private final AuthorizationPort authorizationPort;
    private final PermissionRepositoryPort permissionRepository;
    private final AuthenticationFreshnessEvaluator freshnessEvaluator;

    public SensitiveOperationGuard(AuthorizationPort authorizationPort,
                                   PermissionRepositoryPort permissionRepository,
                                   AuthenticationFreshnessEvaluator freshnessEvaluator) {
        this.authorizationPort = authorizationPort;
        this.permissionRepository = permissionRepository;
        this.freshnessEvaluator = freshnessEvaluator;
    }

    public AuthorizationDecision authorize(AuthorizationRequest request, Instant authenticatedAt) {
        if (request == null) {
            return AuthorizationDecision.DENY;
        }

        AuthorizationDecision base = authorizationPort.authorize(request);
        if (base != AuthorizationDecision.PERMIT) {
            return base;
        }

        PermissionKey key;
        try {
            key = PermissionKey.of(request.action().toCanonicalString());
        } catch (IllegalArgumentException ex) {
            // Permission key that failed validation should not have passed
            // the authorization check; fail closed.
            return AuthorizationDecision.DENY;
        }

        boolean isSensitive = permissionRepository.findAll().stream()
                .anyMatch(p -> p.key().equals(key) && p.sensitive());

        if (!isSensitive) {
            return AuthorizationDecision.PERMIT;
        }

        if (freshnessEvaluator.isFresh(authenticatedAt)) {
            return AuthorizationDecision.PERMIT;
        }

        log.debug("Sensitive permission {} requires step-up (auth stale)", key);
        return AuthorizationDecision.STEP_UP_REQUIRED;
    }
}