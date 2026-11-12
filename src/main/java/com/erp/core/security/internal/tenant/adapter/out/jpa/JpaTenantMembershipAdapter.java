package com.erp.core.security.internal.tenant.adapter.out.jpa;

import com.erp.core.security.internal.tenant.port.TenantMembershipPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JpaTenantMembershipAdapter implements TenantMembershipPort {

    private final SpringDataTenantMembershipRepository repo;

    public JpaTenantMembershipAdapter(SpringDataTenantMembershipRepository repo) {
        this.repo = repo;
    }

    @Override @Transactional(readOnly = true)
    public Set<UUID> findTenantsForActor(UUID actorId) {
        return new HashSet<>(repo.findTenantIdsByActor(actorId));
    }

    @Override @Transactional(readOnly = true)
    public boolean isMemberOf(UUID actorId, UUID tenantId) {
        return repo.existsByActorIdAndTenantId(actorId, tenantId);
    }

    @Override @Transactional
    public void ensureMembership(UUID actorId, UUID tenantId) {
        if (!repo.existsByActorIdAndTenantId(actorId, tenantId)) {
            repo.save(new TenantMembershipEntity(actorId, tenantId));
        }
    }

    @Override @Transactional
    public void removeMembership(UUID actorId, UUID tenantId) {
        repo.deleteByActorIdAndTenantId(actorId, tenantId);
    }
}