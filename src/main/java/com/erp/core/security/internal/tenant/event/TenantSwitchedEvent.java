package com.erp.core.security.internal.tenant.event;

import java.time.Instant;
import java.util.UUID;

public record TenantSwitchedEvent(
        UUID actorId,
        UUID fromTenant,
        UUID toTenant,
        Instant occurredAt
) {
    public TenantSwitchedEvent {
        if (actorId == null || toTenant == null) {
            throw new IllegalArgumentException("actorId and toTenant are required");
        }
        if (occurredAt == null) occurredAt = Instant.now();
    }
}