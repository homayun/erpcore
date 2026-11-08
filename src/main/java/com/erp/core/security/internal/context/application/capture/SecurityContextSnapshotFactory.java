package com.erp.core.security.internal.context.application.capture;

import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.context.SecurityContextSnapshot;

public class SecurityContextSnapshotFactory {

    public SecurityContextSnapshot create(CurrentActor actor, String correlationId) {
        if (actor == null) {
            throw new IllegalArgumentException("actor is required");
        }
        return new SecurityContextSnapshot(actor, correlationId);
    }
}