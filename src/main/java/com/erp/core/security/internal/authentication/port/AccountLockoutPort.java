package com.erp.core.security.internal.authentication.port;

import java.util.UUID;

/**
 * Tracks failed authentication attempts and locks accounts temporarily.
 *
 * Implementations are swappable via configuration:
 * - InMemoryAccountLockoutAdapter: single-node / dev / test
 * - RedisAccountLockoutAdapter:    multi-node / production (opt-in)
 */
public interface AccountLockoutPort {

    boolean isLocked(UUID actorId);

    void recordFailure(UUID actorId);

    void recordSuccess(UUID actorId);
}