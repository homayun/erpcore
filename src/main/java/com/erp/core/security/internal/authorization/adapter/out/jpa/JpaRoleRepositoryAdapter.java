package com.erp.core.security.internal.authorization.adapter.out.jpa;

import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.RoleRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves effective permissions for an actor by UNION of:
 *   1. Role-based permissions (sec_role_permission via sec_role_assignment)
 *   2. Direct user permissions (sec_user_permission) — direct exceptions
 *
 * Both sources are additive. There is NO "DENY" mechanism; direct permissions
 * can only GRANT additional access, not revoke role-granted access.
 */
public class JpaRoleRepositoryAdapter implements RoleRepositoryPort {

    private final SpringDataRolePermissionRepository rolePermRepo;
    private final SpringDataUserPermissionRepository userPermRepo;

    public JpaRoleRepositoryAdapter(SpringDataRolePermissionRepository rolePermRepo,
                                    SpringDataUserPermissionRepository userPermRepo) {
        this.rolePermRepo = rolePermRepo;
        this.userPermRepo = userPermRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<PermissionKey> resolvePermissions(UUID actorId, UUID tenantId) {
        if (actorId == null || tenantId == null) return Set.of();
        Set<PermissionKey> result = new HashSet<>();
        // Role-based permissions
        for (String key : rolePermRepo.findPermissionKeysByActorAndTenant(actorId, tenantId)) {
            try { result.add(PermissionKey.of(key)); }
            catch (IllegalArgumentException ignored) { }
        }
        // Direct permissions (exceptions)
        if (userPermRepo != null) {
            for (String key : userPermRepo.findPermissionKeysByActor(actorId, tenantId)) {
                try { result.add(PermissionKey.of(key)); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        return result;
    }
}