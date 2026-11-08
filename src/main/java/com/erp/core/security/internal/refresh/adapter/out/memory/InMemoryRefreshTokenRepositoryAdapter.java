package com.erp.core.security.internal.refresh.adapter.out.memory;

import com.erp.core.security.internal.refresh.domain.RefreshTokenRecord;
import com.erp.core.security.internal.refresh.port.RefreshTokenRepositoryPort;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryRefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final ConcurrentMap<String, RefreshTokenRecord> byHash = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, RefreshTokenRecord> byId = new ConcurrentHashMap<>();

    @Override
    public void save(RefreshTokenRecord record) {
        byHash.put(record.tokenHash(), record);
        byId.put(record.id(), record);
    }

    @Override
    public Optional<RefreshTokenRecord> findByHash(String tokenHash) {
        return Optional.ofNullable(byHash.get(tokenHash));
    }

    @Override
    public void revoke(UUID id, Instant revokedAt, UUID replacedBy) {
        RefreshTokenRecord existing = byId.get(id);
        if (existing == null) return;
        RefreshTokenRecord updated = new RefreshTokenRecord(
                existing.id(), existing.actorId(), existing.tenantId(),
                existing.tokenHash(), existing.familyId(),
                existing.createdAt(), existing.expiresAt(),
                revokedAt, replacedBy);
        byId.put(id, updated);
        byHash.put(updated.tokenHash(), updated);
    }

    @Override
    public int revokeFamily(UUID familyId, Instant revokedAt) {
        int count = 0;
        for (RefreshTokenRecord r : byId.values()) {
            if (r.familyId().equals(familyId) && !r.isRevoked()) {
                revoke(r.id(), revokedAt, null);
                count++;
            }
        }
        return count;
    }
}