package com.erp.core.security.internal.authorization.port;

import com.erp.core.security.internal.authorization.domain.PermissionKey;

import java.util.Set;
import java.util.UUID;

/**
 * Resolves effective permissions for an actor within a tenant.
 *
 * Phase 08 ships the port only â€” a JPA adapter is included, but role
 * assignment management lives in the Tenant/Admin module (per blueprint).
 * If no roles are assigned, callers fall back to the actor's authorities.
 */
public interface RoleRepositoryPort {

    /** Effective permissions granted to an actor, via any assigned roles. */
    Set<PermissionKey> resolvePermissions(UUID actorId, UUID tenantId);
}