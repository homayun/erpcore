package com.erp.core.admin.application;

import com.erp.core.admin.config.AdminBootstrapProperties;
import com.erp.core.admin.port.AdminRolePort;
import com.erp.core.security.internal.authentication.domain.Credential;
import com.erp.core.security.internal.authentication.domain.PasswordPolicy;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import com.erp.core.security.internal.authentication.port.PasswordHashingPort;
import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;
import java.util.UUID;

public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String PERM_USER_MANAGE = "ADMIN_USER_MANAGE";
    private static final String PERM_ROLE_MANAGE = "ADMIN_ROLE_MANAGE";
    private static final String PERM_FIELD_RULE_MANAGE = "ADMIN_FIELD_RULE_MANAGE";

    private final AdminBootstrapProperties props;
    private final CredentialRepositoryPort credentialRepo;
    private final PasswordHashingPort hasher;
    private final PasswordPolicy policy;
    private final PermissionRepositoryPort permissionRepo;
    private final AdminRolePort rolePort;

    public AdminBootstrapRunner(AdminBootstrapProperties props,
                                CredentialRepositoryPort credentialRepo,
                                PasswordHashingPort hasher,
                                PasswordPolicy policy,
                                PermissionRepositoryPort permissionRepo,
                                AdminRolePort rolePort) {
        this.props = props;
        this.credentialRepo = credentialRepo;
        this.hasher = hasher;
        this.policy = policy;
        this.permissionRepo = permissionRepo;
        this.rolePort = rolePort;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.enabled()) return;
        if (props.tenantId() == null || isBlank(props.username()) || isBlank(props.password())) {
            log.warn("Admin bootstrap enabled but incomplete config; skipping");
            return;
        }
        try {
            bootstrap();
        } catch (Exception ex) {
            log.error("Admin bootstrap failed: {}", ex.getMessage(), ex);
        }
    }

    private void bootstrap() {
        log.info("Bootstrapping admin user for tenant={}", props.tenantId());

        permissionRepo.upsertAll(List.of(
                new PermissionDefinition(PermissionKey.of(PERM_USER_MANAGE),
                        "ADMIN", "Manage users", true),
                new PermissionDefinition(PermissionKey.of(PERM_ROLE_MANAGE),
                        "ADMIN", "Manage roles", true),
                new PermissionDefinition(PermissionKey.of(PERM_FIELD_RULE_MANAGE),
                        "ADMIN", "Manage field authorization rules", true)
        ));

        UUID roleId = rolePort.ensureRole(props.tenantId(), ROLE_ADMIN, 100);
        rolePort.grantPermission(roleId, PERM_USER_MANAGE);
        rolePort.grantPermission(roleId, PERM_ROLE_MANAGE);
        rolePort.grantPermission(roleId, PERM_FIELD_RULE_MANAGE);

        var existing = credentialRepo.findByUsernameAndTenant(props.tenantId(), props.username());
        UUID actorId;
        if (existing.isEmpty()) {
            policy.validate(props.password());
            actorId = UUID.randomUUID();
            credentialRepo.save(new Credential(actorId, props.tenantId(), props.username(),
                    hasher.hash(props.password()), true));
            log.info("Created bootstrap admin user: {}", props.username());
        } else {
            actorId = existing.get().actorId();
            log.debug("Bootstrap admin user already exists: {}", props.username());
        }

        rolePort.ensureAssignment(actorId, roleId, props.tenantId());
        log.info("Bootstrap complete: admin user ready (role={}, actorId={})", ROLE_ADMIN, actorId);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}