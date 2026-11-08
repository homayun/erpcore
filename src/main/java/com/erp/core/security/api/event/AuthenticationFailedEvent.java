package com.erp.core.security.api.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after an authentication attempt is rejected.
 *
 * NEVER includes the submitted password or hash.
 */
public record AuthenticationFailedEvent(
        UUID tenantId,
        String principal,
        AuthenticationFailureReason reason,
        Instant occurredAt
) {
    public AuthenticationFailedEvent {
        if (reason == null) {
            throw new IllegalArgumentException("reason is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
    }
}