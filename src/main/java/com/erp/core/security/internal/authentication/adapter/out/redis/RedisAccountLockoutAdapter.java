package com.erp.core.security.internal.authentication.adapter.out.redis;

import com.erp.core.security.internal.authentication.port.AccountLockoutPort;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

/**
 * Distributed account lockout backed by Redis. Required for multi-node
 * deployments so all instances share lock state.
 *
 * Enable via: erp.security.redis.enabled=true
 */
public class RedisAccountLockoutAdapter implements AccountLockoutPort {

    private static final String COUNTER_KEY = "lockout_count:";
    private static final String LOCK_KEY = "lockout:";

    private final StringRedisTemplate redis;
    private final SecurityProperties props;

    public RedisAccountLockoutAdapter(StringRedisTemplate redis, SecurityProperties props) {
        this.redis = redis;
        this.props = props;
    }

    @Override
    public boolean isLocked(UUID actorId) {
        return Boolean.TRUE.equals(redis.hasKey(LOCK_KEY + actorId));
    }

    @Override
    public void recordFailure(UUID actorId) {
        String counterKey = COUNTER_KEY + actorId;
        Long count = redis.opsForValue().increment(counterKey);
        if (count != null && count == 1L) {
            redis.expire(counterKey, props.lockout().lockDuration());
        }
        if (count != null && count >= props.lockout().maxFailedAttempts()) {
            redis.opsForValue().set(LOCK_KEY + actorId, "locked", props.lockout().lockDuration());
            redis.delete(counterKey);
        }
    }

    @Override
    public void recordSuccess(UUID actorId) {
        redis.delete(COUNTER_KEY + actorId);
    }
}