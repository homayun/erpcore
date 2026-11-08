package com.erp.core.security.internal.authorization.adapter.out.jpa;

import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.RoleRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JpaRoleRepositoryAdapter implements RoleRepositoryPort {

    private final SpringDataRolePermissionRepository repo;

    public JpaRoleRepositoryAdapter(SpringDataRolePermissionRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<PermissionKey> resolvePermissions(UUID actorId, UUID tenantId) {
        if (actorId == null || tenantId == null) return Set.of();
        return new HashSet<>(repo.findPermissionKeysByActorAndTenant(actorId, tenantId)
                .stream()
                .map(PermissionKey::of)
                .toList());
    }
}