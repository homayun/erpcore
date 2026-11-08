package com.erp.core.security.internal.authentication.adapter.out.memory;

import com.erp.core.security.internal.authentication.port.AccountLockoutPort;
import com.erp.core.security.internal.platform.config.SecurityProperties;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory account lockout. State is per-JVM â€” suitable for single-node
 * deployments, development and tests only.
 */
public class InMemoryAccountLockoutAdapter implements AccountLockoutPort {

    private final SecurityProperties props;
    private final Clock clock;
    private final ConcurrentMap<UUID, State> state = new ConcurrentHashMap<>();

    public InMemoryAccountLockoutAdapter(SecurityProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    @Override
    public boolean isLocked(UUID actorId) {
        State s = state.get(actorId);
        return s != null && s.lockedUntil != null && clock.instant().isBefore(s.lockedUntil);
    }

    @Override
    public void recordFailure(UUID actorId) {
        Instant now = clock.instant();
        State s = state.computeIfAbsent(actorId, k -> new State());
        s.failedAttempts++;
        if (s.failedAttempts >= props.lockout().maxFailedAttempts()) {
            s.lockedUntil = now.plus(props.lockout().lockDuration());
        }
    }

    @Override
    public void recordSuccess(UUID actorId) {
        state.remove(actorId);
    }

    private static final class State {
        int failedAttempts;
        Instant lockedUntil;
    }
}