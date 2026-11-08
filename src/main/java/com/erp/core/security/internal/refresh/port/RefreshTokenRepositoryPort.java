package com.erp.core.security.internal.refresh.port;

import com.erp.core.security.internal.refresh.domain.RefreshTokenRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {
    void save(RefreshTokenRecord record);
    Optional<RefreshTokenRecord> findByHash(String tokenHash);
    void revoke(UUID id, Instant revokedAt, UUID replacedBy);
    int revokeFamily(UUID familyId, Instant revokedAt);
}