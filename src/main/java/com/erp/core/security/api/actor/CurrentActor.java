package com.erp.core.security.api.actor;

import java.util.Set;
import java.util.UUID;

public record CurrentActor(
        UUID id,
        ActorType type,
        UUID tenantId,
        String principal,
        Set<String> authorities
) {
    public CurrentActor {
        if (type == null) {
            throw new IllegalArgumentException("Actor type is required");
        }
        if (type != ActorType.ANONYMOUS && id == null) {
            throw new IllegalArgumentException("Authenticated actor requires an id");
        }
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
    }

    public boolean isAuthenticated() {
        return type != ActorType.ANONYMOUS;
    }
}