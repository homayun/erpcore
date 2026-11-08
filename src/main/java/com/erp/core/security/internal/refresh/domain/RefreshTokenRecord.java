package com.erp.core.security.internal.refresh.domain;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenRecord(
        UUID id,
        UUID actorId,
        UUID tenantId,
        String tokenHash,
        UUID familyId,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        UUID replacedBy
) {
    public boolean isRevoked() { return revokedAt != null; }
    public boolean isExpired(Instant now) { return now.isAfter(expiresAt); }
}