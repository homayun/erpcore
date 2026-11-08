package com.erp.core.security.internal.authorization.domain;

import com.erp.core.security.api.authorization.AuthorizationDecision;

/**
 * Internal decision with reason (for audit; not exposed on public API).
 */
public record AuthorizationDecisionRecord(
        AuthorizationDecision decision,
        String reason
) {
    public static AuthorizationDecisionRecord permit(String reason) {
        return new AuthorizationDecisionRecord(AuthorizationDecision.PERMIT, reason);
    }
    public static AuthorizationDecisionRecord deny(String reason) {
        return new AuthorizationDecisionRecord(AuthorizationDecision.DENY, reason);
    }
    public static AuthorizationDecisionRecord stepUp(String reason) {
        return new AuthorizationDecisionRecord(AuthorizationDecision.STEP_UP_REQUIRED, reason);
    }
}