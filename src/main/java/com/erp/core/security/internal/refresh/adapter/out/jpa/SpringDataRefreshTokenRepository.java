package com.erp.core.security.internal.refresh.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    java.util.Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenEntity e SET e.revokedAt = :now WHERE e.familyId = :familyId AND e.revokedAt IS NULL")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    /**
     * Purge tokens whose expiry is before the given instant. These tokens
     * can never authenticate â€” they're expired and unrecoverable.
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity e WHERE e.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);

    /**
     * Purge revoked tokens older than the retention window. Kept temporarily
     * so that a replayed token is recognised as revoked (reuse detection)
     * rather than "invalid".
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity e WHERE e.revokedAt IS NOT NULL AND e.revokedAt < :cutoff")
    int deleteStaleRevokedBefore(@Param("cutoff") Instant cutoff);
}