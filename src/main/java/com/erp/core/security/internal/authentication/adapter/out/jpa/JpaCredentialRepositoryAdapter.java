package com.erp.core.security.internal.authentication.adapter.out.jpa;

import com.erp.core.security.internal.authentication.domain.Credential;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaCredentialRepositoryAdapter implements CredentialRepositoryPort {

    private final SpringDataCredentialRepository repository;

    public JpaCredentialRepositoryAdapter(SpringDataCredentialRepository repository) {
        this.repository = repository;
    }

    @Override @Transactional(readOnly = true)
    public Optional<Credential> findByUsernameAndTenant(UUID tenantId, String username) {
        return repository.findByTenantIdAndUsername(tenantId, username).map(CredentialEntity::toDomain);
    }

    @Override @Transactional
    public void save(Credential credential) {
        repository.save(CredentialEntity.fromDomain(credential));
    }

    @Override @Transactional
    public void updatePasswordHash(UUID actorId, String newHash) {
        repository.findById(actorId).ifPresent(e -> { e.setPasswordHash(newHash); repository.save(e); });
    }

    @Override @Transactional(readOnly = true)
    public List<Credential> findByTenant(UUID tenantId, int page, int size) {
        return repository.findByTenantId(tenantId, PageRequest.of(page, size))
                .stream().map(CredentialEntity::toDomain).toList();
    }

    @Override @Transactional(readOnly = true)
    public long countByTenant(UUID tenantId) { return repository.countByTenantId(tenantId); }

    @Override @Transactional(readOnly = true)
    public boolean existsByUsername(UUID tenantId, String username) {
        return repository.existsByTenantIdAndUsername(tenantId, username);
    }

    @Override @Transactional
    public void setEnabled(UUID actorId, UUID tenantId, boolean enabled) {
        repository.findById(actorId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .ifPresent(e -> { e.setEnabled(enabled); repository.save(e); });
    }
}