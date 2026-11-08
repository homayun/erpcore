package com.erp.core.security.api.authorization;

public interface AuthorizationPort {
    AuthorizationDecision authorize(AuthorizationRequest request);

    FieldAccessDecision authorizeFieldAccess(FieldAccessRequest request);
}