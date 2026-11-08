package com.erp.core.security.internal.authentication.adapter.out.memory;

import com.erp.core.security.internal.authentication.domain.Credential;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryCredentialRepositoryAdapter implements CredentialRepositoryPort {

    private final ConcurrentMap<String, Credential> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Credential> findByUsernameAndTenant(UUID tenantId, String username) {
        if (tenantId == null || username == null) return Optional.empty();
        return Optional.ofNullable(store.get(key(tenantId, username)));
    }

    @Override
    public void save(Credential credential) {
        store.put(key(credential.tenantId(), credential.username()), credential);
    }

    @Override
    public void updatePasswordHash(UUID actorId, String newHash) {
        if (actorId == null || newHash == null || newHash.isBlank()) return;
        store.replaceAll((k, v) -> v.actorId().equals(actorId)
                ? new Credential(v.actorId(), v.tenantId(), v.username(), newHash, v.enabled())
                : v);
    }

    @Override
    public List<Credential> findByTenant(UUID tenantId, int page, int size) {
        int skip = page * size;
        return store.values().stream()
                .filter(c -> c.tenantId().equals(tenantId))
                .skip(skip).limit(size).toList();
    }

    @Override
    public long countByTenant(UUID tenantId) {
        return store.values().stream().filter(c -> c.tenantId().equals(tenantId)).count();
    }

    @Override
    public boolean existsByUsername(UUID tenantId, String username) {
        return store.containsKey(key(tenantId, username));
    }

    @Override
    public void setEnabled(UUID actorId, UUID tenantId, boolean enabled) {
        store.replaceAll((k, v) -> v.actorId().equals(actorId) && v.tenantId().equals(tenantId)
                ? new Credential(v.actorId(), v.tenantId(), v.username(), v.passwordHash(), enabled)
                : v);
    }

    private String key(UUID tenantId, String username) {
        return tenantId + ":" + username.toLowerCase();
    }
}