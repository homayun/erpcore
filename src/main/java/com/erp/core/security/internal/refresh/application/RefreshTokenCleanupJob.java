package com.erp.core.security.internal.refresh.application;

import com.erp.core.security.internal.platform.config.SecurityProperties;
import com.erp.core.security.internal.refresh.adapter.out.jpa.SpringDataRefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Scheduled purge of expired and stale revoked refresh tokens.
 *
 * Two separate purges:
 * <ul>
 *   <li><b>Expired</b> â€” token past its natural expiry; useless, delete now.</li>
 *   <li><b>Stale revoked</b> â€” token revoked more than {@code retention} ago.
 *       Kept temporarily so that a replayed token is detected as <em>reused</em>
 *       (revokes the family) instead of falling through as "invalid".</li>
 * </ul>
 *
 * Scheduling: Spring Boot's {@code @EnableScheduling} must be active.
 * This module enables it automatically via a conditional configuration â€”
 * disable with {@code erp.security.cleanup.enabled=false}.
 */
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final SpringDataRefreshTokenRepository repository;
    private final SecurityProperties props;
    private final Clock clock;

    public RefreshTokenCleanupJob(SpringDataRefreshTokenRepository repository,
                                  SecurityProperties props,
                                  Clock clock) {
        this.repository = repository;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(cron = "${erp.security.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpired() {
        Instant now = clock.instant();
        int expired = repository.deleteExpiredBefore(now);
        Instant retentionCutoff = now.minus(props.cleanup().retention());
        int staleRevoked = repository.deleteStaleRevokedBefore(retentionCutoff);

        if (expired > 0 || staleRevoked > 0) {
            log.info("Refresh token cleanup: removed {} expired, {} stale revoked (retention={})",
                    expired, staleRevoked, props.cleanup().retention());
        }
    }
}