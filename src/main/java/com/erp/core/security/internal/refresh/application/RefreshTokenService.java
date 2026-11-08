package com.erp.core.security.internal.refresh.application;

import com.erp.core.security.internal.platform.config.SecurityProperties;
import com.erp.core.security.internal.refresh.domain.RefreshTokenRecord;
import com.erp.core.security.internal.refresh.port.RefreshTokenRepositoryPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh tokens are opaque random strings (not JWTs). Only their SHA-256
 * hash is persisted, so a DB dump does not yield usable tokens.
 *
 * Rotation on every refresh. If a revoked token is presented again, the
 * whole family is revoked (assume token theft).
 */
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepositoryPort repository;
    private final SecurityProperties props;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepositoryPort repository,
                               SecurityProperties props, Clock clock) {
        this.repository = repository;
        this.props = props;
        this.clock = clock;
    }

    /** Issue a brand-new token family (on login). */
    public IssuedRefreshToken issue(UUID actorId, UUID tenantId) {
        UUID familyId = UUID.randomUUID();
        return issueNext(actorId, tenantId, familyId);
    }

    /** Rotate. Returns new token; revokes old. Detects reuse. */
    public RotatedRefreshToken rotate(String presentedRawToken) {
        String hash = sha256(presentedRawToken);
        RefreshTokenRecord existing = repository.findByHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("not found"));

        Instant now = clock.instant();

        if (existing.isRevoked()) {
            // Reuse detected â€” revoke entire family
            repository.revokeFamily(existing.familyId(), now);
            throw new RefreshTokenReuseDetectedException(existing.familyId());
        }
        if (existing.isExpired(now)) {
            repository.revoke(existing.id(), now, null);
            throw new InvalidRefreshTokenException("expired");
        }

        IssuedRefreshToken next = issueNext(existing.actorId(), existing.tenantId(), existing.familyId());
        repository.revoke(existing.id(), now, next.record().id());

        return new RotatedRefreshToken(existing.actorId(), existing.tenantId(), next.rawToken());
    }

    public void revoke(String presentedRawToken) {
        String hash = sha256(presentedRawToken);
        repository.findByHash(hash).ifPresent(r ->
                repository.revokeFamily(r.familyId(), clock.instant()));
    }

    private IssuedRefreshToken issueNext(UUID actorId, UUID tenantId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        RefreshTokenRecord record = new RefreshTokenRecord(
                UUID.randomUUID(), actorId, tenantId, sha256(raw), familyId,
                now, now.plus(props.refreshToken().ttl()), null, null);
        repository.save(record);
        return new IssuedRefreshToken(raw, record);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record IssuedRefreshToken(String rawToken, RefreshTokenRecord record) {}
    public record RotatedRefreshToken(UUID actorId, UUID tenantId, String rawToken) {}

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String msg) { super(msg); }
    }
    public static class RefreshTokenReuseDetectedException extends RuntimeException {
        private final UUID familyId;
        public RefreshTokenReuseDetectedException(UUID familyId) {
            super("Refresh token reuse detected");
            this.familyId = familyId;
        }
        public UUID getFamilyId() { return familyId; }
    }
}