package com.erp.core.security.api.audit;

import java.time.Instant;
import java.util.UUID;

public record SecurityAuditEvent(
        UUID eventId,
        UUID actorId,
        UUID tenantId,
        String action,
        SecurityAuditSeverity severity,
        Instant timestamp
) {
    public SecurityAuditEvent {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Audit action is required");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Audit severity is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Audit timestamp is required");
        }
    }
}