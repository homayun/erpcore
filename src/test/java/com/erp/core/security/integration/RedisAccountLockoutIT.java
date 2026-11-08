package com.erp.core.security.integration;

import com.erp.core.security.SecurityModuleTestApplication;
import com.erp.core.security.internal.authentication.port.AccountLockoutPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = SecurityModuleTestApplication.class)
class RedisAccountLockoutIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        r.add("erp.security.redis.enabled", () -> "true");
        r.add("erp.security.persistence.type", () -> "memory");
    }

    @Autowired
    private AccountLockoutPort lockout;

    @Test
    void locksAfterThreshold() {
        UUID actor = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            lockout.recordFailure(actor);
        }
        assertThat(lockout.isLocked(actor)).isTrue();
    }

    @Test
    void successClearsFailures() {
        UUID actor = UUID.randomUUID();
        lockout.recordFailure(actor);
        lockout.recordSuccess(actor);
        assertThat(lockout.isLocked(actor)).isFalse();
    }
}