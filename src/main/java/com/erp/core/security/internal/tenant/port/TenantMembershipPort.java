package com.erp.core.security.internal.tenant.port;

import java.util.Set;
import java.util.UUID;

/**
 * Multi-tenant membership store.
 *
 * SECURITY: every tenant override coming from a session MUST be validated
 * against this port before it is used. It is the single source of truth for
 * "actor X is allowed to operate in tenant Y".
 */
public interface TenantMembershipPort {

    Set<UUID> findTenantsForActor(UUID actorId);

    boolean isMemberOf(UUID actorId, UUID tenantId);

    void ensureMembership(UUID actorId, UUID tenantId);

    void removeMembership(UUID actorId, UUID tenantId);
}