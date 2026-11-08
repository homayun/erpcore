package com.erp.core.security.internal.authentication.application;

import com.erp.core.security.api.event.AuthenticationFailureReason;

import java.util.UUID;

public sealed interface AuthenticationResult {

    record Success(UUID actorId, UUID tenantId, String principal) implements AuthenticationResult {
        public Success {
            if (actorId == null) throw new IllegalArgumentException("actorId is required");
            if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
            if (principal == null || principal.isBlank()) {
                throw new IllegalArgumentException("principal is required");
            }
        }
    }

    record Failure(AuthenticationFailureReason reason) implements AuthenticationResult {
        public Failure {
            if (reason == null) throw new IllegalArgumentException("reason is required");
        }
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }
}