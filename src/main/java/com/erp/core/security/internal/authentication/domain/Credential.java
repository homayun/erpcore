package com.erp.core.security.internal.authentication.domain;

import java.util.UUID;

public record Credential(
        UUID actorId,
        UUID tenantId,
        String username,
        String passwordHash,
        boolean enabled
) {
    public Credential {
        if (actorId == null) throw new IllegalArgumentException("actorId is required");
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username is required");
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("passwordHash is required");
    }

    /** Backward-compatible ctor: defaults enabled=true. */
    public Credential(UUID actorId, UUID tenantId, String username, String passwordHash) {
        this(actorId, tenantId, username, passwordHash, true);
    }
}