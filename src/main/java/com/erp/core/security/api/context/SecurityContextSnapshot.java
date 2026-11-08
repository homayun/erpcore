package com.erp.core.security.api.context;

import com.erp.core.security.api.actor.CurrentActor;

public record SecurityContextSnapshot(
        CurrentActor actor,
        String correlationId
) {
    public SecurityContextSnapshot {
        if (actor == null) {
            throw new IllegalArgumentException("Actor is required in snapshot");
        }
    }
}