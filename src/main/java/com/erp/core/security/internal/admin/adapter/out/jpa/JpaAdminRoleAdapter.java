package com.erp.core.security.internal.admin.adapter.out.jpa;

import com.erp.core.security.internal.admin.port.AdminRolePort;
import com.erp.core.security.internal.authorization.adapter.out.jpa.RoleAssignmentEntity;
import com.erp.core.security.internal.authorization.adapter.out.jpa.RolePermissionEntity;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataRoleAssignmentRepository;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataRolePermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JpaAdminRoleAdapter implements AdminRolePort {

    private static final Logger log = LoggerFactory.getLogger(JpaAdminRoleAdapter.class);

    private final SpringDataAdminRoleRepository roleRepo;
    private final SpringDataRolePermissionRepository rolePermissionRepo;
    private final SpringDataRoleAssignmentRepository roleAssignmentRepo;

    public JpaAdminRoleAdapter(SpringDataAdminRoleRepository roleRepo,
                               SpringDataRolePermissionRepository rolePermissionRepo,
                               SpringDataRoleAssignmentRepository roleAssignmentRepo) {
        this.roleRepo = roleRepo;
        this.rolePermissionRepo = rolePermissionRepo;
        this.roleAssignmentRepo = roleAssignmentRepo;
    }

    @Override @Transactional
    public UUID ensureRole(UUID tenantId, String name, int hierarchyLevel) {
        var existing = roleRepo.findByTenantIdAndName(tenantId, name);
        if (existing.isPresent()) return existing.get().getId();
        UUID id = UUID.randomUUID();
        roleRepo.save(RoleEntity.of(id, tenantId, name, hierarchyLevel));
        log.debug("ensureRole: inserted {} id={}", name, id);
        return id;
    }

    @Override @Transactional
    public UUID createRole(UUID tenantId, String name, int hierarchyLevel) {
        if (roleRepo.findByTenantIdAndName(tenantId, name).isPresent()) {
            throw new IllegalStateException("Role already exists: " + name);
        }
        UUID id = UUID.randomUUID();
        roleRepo.save(RoleEntity.of(id, tenantId, name, hierarchyLevel));
        return id;
    }

    @Override @Transactional
    public void grantPermission(UUID roleId, String permissionKey) {
        if (!rolePermissionRepo.existsByRoleIdAndPermissionKey(roleId, permissionKey)) {
            rolePermissionRepo.save(new RolePermissionEntity(roleId, permissionKey));
            log.debug("grantPermission: role={} perm={}", roleId, permissionKey);
        }
    }

    @Override @Transactional
    public void revokePermission(UUID roleId, String permissionKey) {
        rolePermissionRepo.deleteByRoleIdAndPermissionKey(roleId, permissionKey);
    }

    @Override @Transactional
    public void ensureAssignment(UUID actorId, UUID roleId, UUID tenantId) {
        if (!roleAssignmentRepo.existsByActorIdAndRoleId(actorId, roleId)) {
            roleAssignmentRepo.save(RoleAssignmentEntity.of(
                    UUID.randomUUID(), actorId, roleId, tenantId));
            log.debug("ensureAssignment: actor={} role={}", actorId, roleId);
        }
    }

    @Override @Transactional
    public void assignRole(UUID actorId, UUID roleId, UUID tenantId) {
        ensureAssignment(actorId, roleId, tenantId);
    }

    @Override @Transactional(readOnly = true)
    public List<RoleView> listRoles(UUID tenantId) {
        return roleRepo.findByTenantId(tenantId).stream().map(r -> {
            var perms = new HashSet<String>();
            rolePermissionRepo.findAll().stream()
                    .filter(rp -> rp.getRoleId().equals(r.getId()))
                    .forEach(rp -> perms.add(rp.getPermissionKey()));
            return new RoleView(r.getId(), r.getTenantId(), r.getName(),
                    r.getHierarchyLevel(), perms);
        }).collect(Collectors.toList());
    }
}