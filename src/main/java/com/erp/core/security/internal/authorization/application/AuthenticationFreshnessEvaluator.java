package com.erp.core.security.internal.authorization.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Evaluates whether a caller's authentication is recent enough to perform
 * a sensitive operation.
 *
 * Fail-closed on every edge case:
 * - null timestamp                -> not fresh
 * - future timestamp (clock skew) -> not fresh (defensive)
 * - negative elapsed              -> not fresh
 *
 * The "freshness window" is supplied by configuration. A 15-minute default
 * is typical for ERP approval flows (matches most enterprise step-up policies).
 */
public class AuthenticationFreshnessEvaluator {

    private final Clock clock;
    private final Duration maxAge;

    public AuthenticationFreshnessEvaluator(Clock clock, Duration maxAge) {
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }
        if (maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("maxAge must be positive");
        }
        this.clock = clock;
        this.maxAge = maxAge;
    }

    public boolean isFresh(Instant authenticatedAt) {
        if (authenticatedAt == null) {
            return false;
        }
        Duration elapsed = Duration.between(authenticatedAt, clock.instant());
        if (elapsed.isNegative()) {
            // Authenticated in the future — treat as suspicious, deny.
            return false;
        }
        return elapsed.compareTo(maxAge) <= 0;
    }
}