package com.erp.core.security.internal.context.application.capture;

import com.erp.core.security.api.actor.ActorType;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.context.SecurityContextSnapshot;
import com.erp.core.security.internal.data.application.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextCaptureServiceTest {

    private final SecurityContextCaptureService service =
            new SecurityContextCaptureService(new SecurityContextSnapshotFactory());

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void captureReturnsSnapshot() {
        CurrentActor actor = userActor(UUID.randomUUID());
        SecurityContextSnapshot snapshot = service.capture(actor, "corr-1");
        assertThat(snapshot.actor()).isSameAs(actor);
        assertThat(snapshot.correlationId()).isEqualTo("corr-1");
    }

    @Test
    void restoreBindsTenant() {
        UUID tenantId = UUID.randomUUID();
        SecurityContextSnapshot snapshot =
                service.capture(userActor(tenantId), "corr-2");

        service.restore(snapshot);

        assertThat(TenantContextHolder.current()).contains(tenantId);
    }

    @Test
    void restoreWithNullIsNoOp() {
        service.restore(null);
        assertThat(TenantContextHolder.current()).isEmpty();
    }

    @Test
    void clearRemovesTenant() {
        TenantContextHolder.bind(UUID.randomUUID());
        service.clear();
        assertThat(TenantContextHolder.current()).isEmpty();
    }

    private CurrentActor userActor(UUID tenantId) {
        return new CurrentActor(
                UUID.randomUUID(),
                ActorType.USER,
                tenantId,
                "user@example.com",
                Set.of("ROLE_USER")
        );
    }
}