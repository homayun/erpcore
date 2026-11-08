package com.erp.core.security.internal.refresh.adapter.out.jpa;

import com.erp.core.security.internal.refresh.domain.RefreshTokenRecord;
import com.erp.core.security.internal.refresh.port.RefreshTokenRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final SpringDataRefreshTokenRepository repo;

    public JpaRefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void save(RefreshTokenRecord record) {
        repo.save(RefreshTokenEntity.fromDomain(record));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshTokenRecord> findByHash(String tokenHash) {
        return repo.findByTokenHash(tokenHash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    @Transactional
    public void revoke(UUID id, Instant revokedAt, UUID replacedBy) {
        repo.findById(id).ifPresent(e -> {
            e.setRevokedAt(revokedAt);
            e.setReplacedBy(replacedBy);
            repo.save(e);
        });
    }

    @Override
    @Transactional
    public int revokeFamily(UUID familyId, Instant revokedAt) {
        return repo.revokeFamily(familyId, revokedAt);
    }
}