package com.erp.core.security.internal.abuse.adapter.out.memory;

import com.erp.core.security.internal.abuse.port.RateLimitPort;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory rate limiter. Per-JVM state â€” single-node only.
 */
public class InMemoryRateLimitAdapter implements RateLimitPort {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final Duration refill;

    public InMemoryRateLimitAdapter(long capacity, Duration refill) {
        this.capacity = capacity;
        this.refill = refill;
    }

    @Override
    public boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillGreedy(capacity, refill)
                                .build())
                        .build());
        return bucket.tryConsume(1);
    }
}