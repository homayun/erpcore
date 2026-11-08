package com.erp.core.security.internal.refresh;

import com.erp.core.security.internal.platform.config.SecurityProperties;
import com.erp.core.security.internal.refresh.adapter.out.memory.InMemoryRefreshTokenRepositoryAdapter;
import com.erp.core.security.internal.refresh.application.RefreshTokenService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenServiceTest {

    private final SecurityProperties props = new SecurityProperties(
            true,
            "X-Tenant-Id",
            null,   // persistence
            null,   // login
            null,   // logout
            null,   // jwt
            new SecurityProperties.RefreshToken(Duration.ofDays(30), "erp_refresh"),
            null,   // lockout
            null,   // cleanup
            null    // redis
    );

    @Test
    void issueAndRotate() {
        var repo = new InMemoryRefreshTokenRepositoryAdapter();
        var svc = new RefreshTokenService(repo, props, Clock.systemUTC());

        var issued = svc.issue(UUID.randomUUID(), UUID.randomUUID());
        var rotated = svc.rotate(issued.rawToken());

        assertThat(rotated.rawToken()).isNotEqualTo(issued.rawToken());
    }

    @Test
    void reuseDetectionRevokesFamily() {
        var repo = new InMemoryRefreshTokenRepositoryAdapter();
        var svc = new RefreshTokenService(repo, props, Clock.systemUTC());

        var issued = svc.issue(UUID.randomUUID(), UUID.randomUUID());
        svc.rotate(issued.rawToken());

        assertThatThrownBy(() -> svc.rotate(issued.rawToken()))
                .isInstanceOf(RefreshTokenService.RefreshTokenReuseDetectedException.class);
    }

    @Test
    void expiredTokenRejected() {
        var repo = new InMemoryRefreshTokenRepositoryAdapter();
        Clock past = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        var svc = new RefreshTokenService(repo, props, past);

        var issued = svc.issue(UUID.randomUUID(), UUID.randomUUID());

        Clock future = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        var svcFuture = new RefreshTokenService(repo, props, future);

        assertThatThrownBy(() -> svcFuture.rotate(issued.rawToken()))
                .isInstanceOf(RefreshTokenService.InvalidRefreshTokenException.class);
    }
}