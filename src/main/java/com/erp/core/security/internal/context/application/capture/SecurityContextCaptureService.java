package com.erp.core.security.internal.context.application.capture;

import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.context.SecurityContextSnapshot;
import com.erp.core.security.internal.data.application.TenantContextHolder;

import java.util.UUID;

public class SecurityContextCaptureService {

    private final SecurityContextSnapshotFactory snapshotFactory;

    public SecurityContextCaptureService(SecurityContextSnapshotFactory snapshotFactory) {
        this.snapshotFactory = snapshotFactory;
    }

    public SecurityContextSnapshot capture(CurrentActor actor, String correlationId) {
        return snapshotFactory.create(actor, correlationId);
    }

    public void restore(SecurityContextSnapshot snapshot) {
        if (snapshot == null || snapshot.actor() == null) {
            return;
        }
        UUID tenantId = snapshot.actor().tenantId();
        if (tenantId != null) {
            TenantContextHolder.bind(tenantId);
        }
    }

    public void clear() {
        TenantContextHolder.clear();
    }
}