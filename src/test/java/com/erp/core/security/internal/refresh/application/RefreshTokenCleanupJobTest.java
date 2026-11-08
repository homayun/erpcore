package com.erp.core.security.internal.refresh.application;

import com.erp.core.security.internal.platform.config.SecurityProperties;
import com.erp.core.security.internal.refresh.adapter.out.jpa.SpringDataRefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupJobTest {

    @Mock SpringDataRefreshTokenRepository repo;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-06-01T03:00:00Z"), ZoneOffset.UTC);

    private final SecurityProperties props = new SecurityProperties(
            true, "X-Tenant-Id", null, null, null, null, null, null,
            new SecurityProperties.Cleanup("0 0 3 * * *", Duration.ofDays(7)),
            null);

    @Test
    void purgeCallsExpiredAndStaleRevoked() {
        when(repo.deleteExpiredBefore(any())).thenReturn(3);
        when(repo.deleteStaleRevokedBefore(any())).thenReturn(2);

        new RefreshTokenCleanupJob(repo, props, clock).purgeExpired();

        verify(repo).deleteExpiredBefore(Instant.parse("2026-06-01T03:00:00Z"));
        verify(repo).deleteStaleRevokedBefore(Instant.parse("2026-05-25T03:00:00Z")); // 7 days earlier
    }
}