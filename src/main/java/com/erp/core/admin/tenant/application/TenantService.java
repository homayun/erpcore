package com.erp.core.admin.tenant.application;

import com.erp.core.admin.adapter.out.jpa.RoleEntity;
import com.erp.core.admin.adapter.out.jpa.SpringDataAdminRoleRepository;
import com.erp.core.admin.tenant.adapter.out.jpa.SpringDataTenantRepository;
import com.erp.core.admin.tenant.adapter.out.jpa.TenantEntity;
import com.erp.core.admin.web.dto.CreateTenantRequest;
import com.erp.core.admin.web.dto.TenantSummary;
import com.erp.core.security.internal.authentication.domain.Credential;
import com.erp.core.security.internal.authentication.domain.PasswordPolicy;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import com.erp.core.security.internal.authentication.port.PasswordHashingPort;
import com.erp.core.security.internal.authorization.adapter.out.jpa.RoleAssignmentEntity;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataRoleAssignmentRepository;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataRolePermissionRepository;
import com.erp.core.security.internal.tenant.port.TenantMembershipPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private static final String ROLE_ADMIN = "ADMIN";
    private static final int    ADMIN_LEVEL = 100;

    private static final List<String> DEFAULT_ADMIN_PERMISSIONS = List.of(
            "ADMIN_USER_MANAGE",
            "ADMIN_ROLE_MANAGE",
            "ADMIN_FIELD_RULE_MANAGE",
            "TENANT_MANAGE"
    );

    private final SpringDataTenantRepository tenantRepo;
    private final TenantMembershipPort membership;
    private final CredentialRepositoryPort credentialRepo;
    private final PasswordHashingPort hasher;
    private final PasswordPolicy policy;
    private final SpringDataAdminRoleRepository roleRepo;
    private final SpringDataRolePermissionRepository rolePermissionRepo;
    private final SpringDataRoleAssignmentRepository roleAssignmentRepo;

    public TenantService(SpringDataTenantRepository tenantRepo,
                         TenantMembershipPort membership,
                         CredentialRepositoryPort credentialRepo,
                         PasswordHashingPort hasher,
                         PasswordPolicy policy,
                         SpringDataAdminRoleRepository roleRepo,
                         SpringDataRolePermissionRepository rolePermissionRepo,
                         SpringDataRoleAssignmentRepository roleAssignmentRepo) {
        this.tenantRepo = tenantRepo;
        this.membership = membership;
        this.credentialRepo = credentialRepo;
        this.hasher = hasher;
        this.policy = policy;
        this.roleRepo = roleRepo;
        this.rolePermissionRepo = rolePermissionRepo;
        this.roleAssignmentRepo = roleAssignmentRepo;
    }

    @Transactional(readOnly = true)
    public List<TenantSummary> list() {
        return tenantRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public TenantSummary getById(UUID id) {
        return tenantRepo.findById(id).map(this::toSummary)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    @Transactional
    public TenantSummary create(CreateTenantRequest req, UUID createdBy) {
        if (isBlank(req.code()))        throw new IllegalArgumentException("code is required");
        if (isBlank(req.name()))        throw new IllegalArgumentException("name is required");
        if (tenantRepo.existsByCodeIgnoreCase(req.code())) {
            throw new IllegalStateException("Tenant code already exists: " + req.code());
        }

        UUID tenantId = UUID.randomUUID();
        TenantEntity entity = TenantEntity.create(
                tenantId, req.code(), req.name(),
                isBlank(req.subscriptionPlan()) ? "STANDARD" : req.subscriptionPlan(),
                createdBy);
        tenantRepo.save(entity);

        // Super-admin who created the tenant becomes a member automatically.
        membership.ensureMembership(createdBy, tenantId);

        // Optional bootstrap admin for the new tenant.
        if (!isBlank(req.initialAdminUsername()) && !isBlank(req.initialAdminPassword())) {
            createInitialAdmin(tenantId, req.initialAdminUsername(), req.initialAdminPassword());
        }

        log.info("Created tenant code={} name={} id={} by={}",
                entity.getCode(), entity.getName(), tenantId, createdBy);
        return toSummary(entity);
    }

    @Transactional
    public TenantSummary updateStatus(UUID id, String newStatus) {
        TenantEntity e = tenantRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        try {
            e.setStatus(TenantEntity.Status.valueOf(newStatus.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status must be ACTIVE, SUSPENDED, or ARCHIVED");
        }
        return toSummary(tenantRepo.save(e));
    }

    // ---------------------------------------------------------------
    // Bootstrap admin creation for a new tenant
    // ---------------------------------------------------------------

    private void createInitialAdmin(UUID tenantId, String username, String rawPassword) {
        policy.validate(rawPassword);
        if (credentialRepo.existsByUsername(tenantId, username)) {
            throw new IllegalStateException("Username already exists in tenant: " + username);
        }
        UUID actorId = UUID.randomUUID();
        credentialRepo.save(new Credential(actorId, tenantId, username,
                hasher.hash(rawPassword), true));
        membership.ensureMembership(actorId, tenantId);

        // Ensure ADMIN role exists in the new tenant, with default permissions.
        UUID roleId = roleRepo.findByTenantIdAndName(tenantId, ROLE_ADMIN)
                .map(RoleEntity::getId)
                .orElseGet(() -> {
                    UUID rid = UUID.randomUUID();
                    roleRepo.save(RoleEntity.of(rid, tenantId, ROLE_ADMIN, ADMIN_LEVEL));
                    return rid;
                });

        for (String perm : DEFAULT_ADMIN_PERMISSIONS) {
            if (!rolePermissionRepo.existsByRoleIdAndPermissionKey(roleId, perm)) {
                rolePermissionRepo.save(
                        new com.erp.core.security.internal.authorization.adapter.out.jpa.RolePermissionEntity(
                                roleId, perm));
            }
        }
        if (!roleAssignmentRepo.existsByActorIdAndRoleId(actorId, roleId)) {
            roleAssignmentRepo.save(RoleAssignmentEntity.of(UUID.randomUUID(), actorId, roleId, tenantId));
        }
        log.info("Bootstrap admin created for new tenant: username={} tenant={}", username, tenantId);
    }

    private TenantSummary toSummary(TenantEntity e) {
        return new TenantSummary(e.getId(), e.getCode(), e.getName(),
                e.getStatus().name(), e.getSubscriptionPlan(), e.getCreatedAt());
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}