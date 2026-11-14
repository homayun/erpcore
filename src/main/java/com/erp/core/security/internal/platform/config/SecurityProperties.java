package com.erp.core.security.internal.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "erp.security")
public record SecurityProperties(
        boolean enabled,
        String defaultTenantHeader,
        Persistence persistence,
        Login login,
        Logout logout,
        Jwt jwt,
        RefreshToken refreshToken,
        Lockout lockout,
        Cleanup cleanup,
        Redis redis
) {
    public SecurityProperties {
        if (defaultTenantHeader == null || defaultTenantHeader.isBlank()) {
            defaultTenantHeader = "X-Tenant-Id";
        }
        if (persistence == null) persistence = new Persistence(null);
        if (login == null) login = new Login(null, null);
        if (logout == null) logout = new Logout(null);
        if (jwt == null) jwt = new Jwt(null, null, null, null);
        if (refreshToken == null) refreshToken = new RefreshToken(null, null);
        if (lockout == null) lockout = new Lockout(5, Duration.ofMinutes(15));
        if (cleanup == null) cleanup = new Cleanup(null, null);
        if (redis == null) redis = new Redis(false);
    }

    public record Persistence(String type) {
        public Persistence {
            if (type == null || type.isBlank()) type = "jpa";
        }
    }

    /**
     * Login configuration.
     *
     * @param processingUrl   the login POST endpoint (default {@code /login}).
     * @param defaultTenantId OPTIONAL UUID applied when the request body does
     *                        not include a {@code tenantId}. For single-tenant
     *                        on-prem deployments set this to the customer's
     *                        tenant UUID. For multi-tenant SaaS leave it blank
     *                        — login fails closed (401) if the client omits
     *                        {@code tenantId}.
     */
    public record Login(String processingUrl, String defaultTenantId) {
        public Login {
            if (processingUrl == null || processingUrl.isBlank()) processingUrl = "/login";
        }
    }

    public record Logout(String logoutUrl) {
        public Logout {
            if (logoutUrl == null || logoutUrl.isBlank()) logoutUrl = "/logout";
        }
    }

    public record Jwt(String issuer, String secret, Duration accessTtl, Duration clockSkew) {
        public Jwt {
            if (issuer == null || issuer.isBlank()) issuer = "erp-core";
            if (secret == null || secret.isBlank()) secret = "change-me-change-me-change-me-32-chars";
            if (accessTtl == null) accessTtl = Duration.ofMinutes(15);
            else if (accessTtl.isNegative() || accessTtl.isZero())
                throw new IllegalArgumentException("erp.security.jwt.access-ttl must be > 0");
            if (clockSkew == null) clockSkew = Duration.ofSeconds(30);
            else if (clockSkew.isNegative())
                throw new IllegalArgumentException("erp.security.jwt.clock-skew must be >= 0");
        }
    }

    public record RefreshToken(Duration ttl, String cookieName) {
        public RefreshToken {
            if (ttl == null) ttl = Duration.ofDays(30);
            else if (ttl.isNegative() || ttl.isZero())
                throw new IllegalArgumentException("erp.security.refresh-token.ttl must be > 0");
            if (cookieName == null || cookieName.isBlank()) cookieName = "erp_refresh";
        }
    }

    public record Lockout(int maxFailedAttempts, Duration lockDuration) {
        public Lockout {
            if (maxFailedAttempts <= 0)
                throw new IllegalArgumentException("erp.security.lockout.max-failed-attempts must be > 0");
            if (lockDuration == null)
                throw new IllegalArgumentException("erp.security.lockout.lock-duration is required");
            if (lockDuration.isNegative() || lockDuration.isZero())
                throw new IllegalArgumentException("erp.security.lockout.lock-duration must be > 0");
        }
    }

    public record Cleanup(String cron, Duration retention) {
        public Cleanup {
            if (cron == null || cron.isBlank()) cron = "0 0 3 * * *";
            if (retention == null) retention = Duration.ofDays(7);
            else if (retention.isNegative() || retention.isZero())
                throw new IllegalArgumentException("erp.security.cleanup.retention must be > 0");
        }
    }

    public record Redis(boolean enabled) { }
}