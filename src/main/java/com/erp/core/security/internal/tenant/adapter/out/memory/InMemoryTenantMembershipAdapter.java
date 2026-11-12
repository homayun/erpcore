package com.erp.core.security.internal.tenant.adapter.out.memory;

import com.erp.core.security.internal.tenant.port.TenantMembershipPort;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryTenantMembershipAdapter implements TenantMembershipPort {

    private final ConcurrentMap<UUID, Set<UUID>> byActor = new ConcurrentHashMap<>();

    @Override
    public Set<UUID> findTenantsForActor(UUID actorId) {
        return Set.copyOf(byActor.getOrDefault(actorId, Set.of()));
    }

    @Override
    public boolean isMemberOf(UUID actorId, UUID tenantId) {
        return byActor.getOrDefault(actorId, Set.of()).contains(tenantId);
    }

    @Override
    public void ensureMembership(UUID actorId, UUID tenantId) {
        byActor.computeIfAbsent(actorId, k -> ConcurrentHashMap.newKeySet()).add(tenantId);
    }

    @Override
    public void removeMembership(UUID actorId, UUID tenantId) {
        Set<UUID> s = byActor.get(actorId);
        if (s != null) s.remove(tenantId);
    }
}