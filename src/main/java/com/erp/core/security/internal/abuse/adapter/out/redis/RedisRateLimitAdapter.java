package com.erp.core.security.internal.abuse.adapter.out.redis;

import com.erp.core.security.internal.abuse.port.RateLimitPort;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Distributed rate limiter backed by Redis. Enable via:
 * erp.security.redis.enabled=true
 */
public class RedisRateLimitAdapter implements RateLimitPort {

    private final ProxyManager<byte[]> proxyManager;
    private final Supplier<BucketConfiguration> config;

    public RedisRateLimitAdapter(ProxyManager<byte[]> proxyManager, long capacity, Duration refill) {
        this.proxyManager = proxyManager;
        this.config = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refill)
                        .build())
                .build();
    }

    @Override
    public boolean tryConsume(String key) {
        byte[] redisKey = ("rate_limit:" + key).getBytes(StandardCharsets.UTF_8);
        return proxyManager.builder().build(redisKey, config).tryConsume(1);
    }
}