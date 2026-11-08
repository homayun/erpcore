package com.erp.core.security.internal.authentication;

import com.erp.core.security.internal.authentication.adapter.out.memory.InMemoryAccountLockoutAdapter;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAccountLockoutAdapterTest {

    private final SecurityProperties props = new SecurityProperties(
            true,
            "X-Tenant-Id",
            null,   // persistence
            null,   // login
            null,   // logout
            null,   // jwt
            null,   // refreshToken
            new SecurityProperties.Lockout(3, Duration.ofMinutes(15)),
            null,   // cleanup
            null    // redis
    );

    @Test
    void locksAfterThreshold() {
        var adapter = new InMemoryAccountLockoutAdapter(props, Clock.systemUTC());
        UUID actorId = UUID.randomUUID();

        adapter.recordFailure(actorId);
        adapter.recordFailure(actorId);
        assertThat(adapter.isLocked(actorId)).isFalse();

        adapter.recordFailure(actorId);
        assertThat(adapter.isLocked(actorId)).isTrue();
    }

    @Test
    void successResetsState() {
        var adapter = new InMemoryAccountLockoutAdapter(props, Clock.systemUTC());
        UUID actorId = UUID.randomUUID();
        adapter.recordFailure(actorId);
        adapter.recordSuccess(actorId);
        assertThat(adapter.isLocked(actorId)).isFalse();
    }
}