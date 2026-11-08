package com.erp.core.security.internal.authorization.adapter.out.jpa;

import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public class JpaPermissionRepositoryAdapter implements PermissionRepositoryPort {

    private final SpringDataPermissionRepository repo;

    public JpaPermissionRepositoryAdapter(SpringDataPermissionRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void upsertAll(Collection<PermissionDefinition> definitions) {
        for (PermissionDefinition d : definitions) {
            String key = d.key().value();
            repo.findById(key).ifPresentOrElse(
                    existing -> {
                        existing.setGroupKey(d.group());
                        existing.setDescription(d.description());
                        existing.setSensitive(d.sensitive());
                        repo.save(existing);
                    },
                    () -> repo.save(PermissionEntity.fromDomain(d)));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(PermissionKey key) {
        return repo.existsById(key.value());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDefinition> findAll() {
        return repo.findAll().stream().map(PermissionEntity::toDomain).toList();
    }
}