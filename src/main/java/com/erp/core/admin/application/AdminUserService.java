package com.erp.core.admin.application;

import com.erp.core.admin.web.dto.CreateUserRequest;
import com.erp.core.admin.web.dto.PageResponse;
import com.erp.core.admin.web.dto.UpdateUserRequest;
import com.erp.core.admin.web.dto.UserSummary;
import com.erp.core.admin.adapter.out.jpa.RoleEntity;
import com.erp.core.admin.adapter.out.jpa.SpringDataAdminRoleRepository;
import com.erp.core.security.internal.authentication.domain.Credential;
import com.erp.core.security.internal.authentication.domain.PasswordPolicy;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import com.erp.core.security.internal.authentication.port.PasswordHashingPort;
import com.erp.core.security.internal.authorization.adapter.out.jpa.RoleAssignmentEntity;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataRoleAssignmentRepository;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataUserPermissionRepository;
import com.erp.core.security.internal.authorization.adapter.out.jpa.UserPermissionEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminUserService {

    private final CredentialRepositoryPort repo;
    private final PasswordHashingPort hasher;
    private final PasswordPolicy policy;
    private final SpringDataAdminRoleRepository roleRepo;
    private final SpringDataRoleAssignmentRepository roleAssignmentRepo;
    private final SpringDataUserPermissionRepository userPermRepo;

    public AdminUserService(CredentialRepositoryPort repo,
                            PasswordHashingPort hasher,
                            PasswordPolicy policy,
                            SpringDataAdminRoleRepository roleRepo,
                            SpringDataRoleAssignmentRepository roleAssignmentRepo,
                            SpringDataUserPermissionRepository userPermRepo) {
        this.repo = repo;
        this.hasher = hasher;
        this.policy = policy;
        this.roleRepo = roleRepo;
        this.roleAssignmentRepo = roleAssignmentRepo;
        this.userPermRepo = userPermRepo;
    }

    public UserSummary create(CreateUserRequest req) {
        if (req.tenantId() == null) throw new IllegalArgumentException("tenantId is required");
        if (req.username() == null || req.username().isBlank()) throw new IllegalArgumentException("username is required");
        policy.validate(req.password());
        if (repo.existsByUsername(req.tenantId(), req.username())) {
            throw new IllegalStateException("username already exists in this tenant");
        }
        UUID actorId = UUID.randomUUID();
        Credential credential = new Credential(actorId, req.tenantId(), req.username(), hasher.hash(req.password()), true);
        repo.save(credential);
        return new UserSummary(actorId, req.tenantId(), req.username(), true);
    }

    public PageResponse<UserSummary> list(UUID tenantId, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 200) size = 20;
        var items = repo.findByTenant(tenantId, page, size).stream()
                .map(c -> new UserSummary(c.actorId(), c.tenantId(), c.username(), c.enabled()))
                .toList();
        long total = repo.countByTenant(tenantId);
        return new PageResponse<>(items, total, page, size);
    }

    public void setEnabled(UUID actorId, UUID tenantId, boolean enabled) {
        repo.setEnabled(actorId, tenantId, enabled);
    }

    public void update(UUID actorId, UUID tenantId, UpdateUserRequest req) {
        String newHash = null;
        if (req.password() != null && !req.password().isBlank()) {
            policy.validate(req.password());
            newHash = hasher.hash(req.password());
        }
        if (req.username() != null && !req.username().isBlank() && repo.existsByUsername(tenantId, req.username())) {
            throw new IllegalStateException("username already exists in this tenant");
        }
        repo.updateUser(actorId, tenantId, req.username(), newHash);
    }

    public void delete(UUID actorId, UUID tenantId) {
        userPermRepo.deleteByActorId(actorId);
        roleAssignmentRepo.deleteByActorId(actorId);
        repo.deleteUser(actorId, tenantId);
    }

    // ---- Roles ----
    public List<RoleEntity> getRoles(UUID tenantId) {
        return roleRepo.findByTenantId(tenantId);
    }

    public List<UUID> getAssignedRoles(UUID actorId, UUID tenantId) {
        return roleAssignmentRepo.findAll().stream()
                .filter(ra -> ra.getActorId().equals(actorId) && ra.getTenantId().equals(tenantId))
                .map(RoleAssignmentEntity::getRoleId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignRoles(UUID actorId, UUID tenantId, List<UUID> roleIds) {
        roleAssignmentRepo.deleteByActorId(actorId);
        if (roleIds != null) {
            for (UUID roleId : roleIds) {
                roleAssignmentRepo.save(RoleAssignmentEntity.of(UUID.randomUUID(), actorId, roleId, tenantId));
            }
        }
    }

    // ---- Direct Permissions ----
    @Transactional(readOnly = true)
    public Set<String> getAssignedDirectPermissions(UUID actorId, UUID tenantId) {
        return new HashSet<>(userPermRepo.findPermissionKeysByActor(actorId, tenantId));
    }

    @Transactional
    public void replaceDirectPermissions(UUID actorId, UUID tenantId, List<String> permissionKeys) {
        userPermRepo.deleteByActorId(actorId);
        userPermRepo.flush();
        if (permissionKeys != null && !permissionKeys.isEmpty()) {
            Set<String> unique = new HashSet<>(permissionKeys);
            for (String key : unique) {
                userPermRepo.save(new UserPermissionEntity(actorId, key, tenantId));
            }
        }
    }
}