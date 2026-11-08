package com.erp.core.security.internal.abuse.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

/**
 * Redis-backed rate limiting infrastructure. Disabled by default.
 * Enable with: erp.security.redis.enabled=true
 *
 * Phase 06 wires the RedisRateLimitService bean behind this configuration
 * and tests it end-to-end with Testcontainers.
 */
@Configuration
@ConditionalOnProperty(name = "erp.security.redis.enabled", havingValue = "true")
public class RedisRateLimitConfiguration {

    @Bean
    @SuppressWarnings("deprecation")
    public LettuceBasedProxyManager<byte[]> proxyManager(RedisConnectionFactory connectionFactory) {
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
        RedisClient redisClient = (RedisClient) lettuceFactory.getNativeClient();

        return LettuceBasedProxyManager.builderFor(redisClient)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy
                                .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
    }
}