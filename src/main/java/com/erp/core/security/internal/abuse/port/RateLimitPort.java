package com.erp.core.security.internal.abuse.port;

/**
 * Per-key rate limiter. Returns true if the caller is allowed to proceed.
 */
public interface RateLimitPort {

    boolean tryConsume(String key);
}