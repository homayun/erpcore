package com.erp.core.security.internal.abuse;

import com.erp.core.security.internal.abuse.adapter.out.memory.InMemoryRateLimitAdapter;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimitAdapterTest {

    @Test
    void allowsUpToCapacity() {
        var adapter = new InMemoryRateLimitAdapter(3, Duration.ofMinutes(1));
        assertThat(adapter.tryConsume("ip:1")).isTrue();
        assertThat(adapter.tryConsume("ip:1")).isTrue();
        assertThat(adapter.tryConsume("ip:1")).isTrue();
        assertThat(adapter.tryConsume("ip:1")).isFalse();
    }

    @Test
    void keysAreIsolated() {
        var adapter = new InMemoryRateLimitAdapter(1, Duration.ofMinutes(1));
        assertThat(adapter.tryConsume("ip:1")).isTrue();
        assertThat(adapter.tryConsume("ip:2")).isTrue();
    }
}