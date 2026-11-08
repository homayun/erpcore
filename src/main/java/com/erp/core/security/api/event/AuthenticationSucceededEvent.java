package com.erp.core.security.api.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a successful authentication.
 *
 * NEVER includes any password, token, or credential material.
 */
public record AuthenticationSucceededEvent(
        UUID actorId,
        UUID tenantId,
        String principal,
        Instant occurredAt
) {
    public AuthenticationSucceededEvent {
        if (actorId == null) {
            throw new IllegalArgumentException("actorId is required");
        }
        if (principal == null || principal.isBlank()) {
            throw new IllegalArgumentException("principal is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
    }
}