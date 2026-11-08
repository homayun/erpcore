package com.erp.core.security.internal.admin.config;

import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.api.authorization.AuthorizationPort;
import com.erp.core.security.internal.admin.adapter.out.jpa.JpaAdminRoleAdapter;
import com.erp.core.security.internal.admin.adapter.out.jpa.SpringDataAdminRoleRepository;
import com.erp.core.security.internal.admin.application.AdminBootstrapRunner;
import com.erp.core.security.internal.admin.application.AdminFieldRuleService;
import com.erp.core.security.internal.admin.application.AdminRoleService;
import com.erp.core.security.internal.admin.application.AdminUserService;
import com.erp.core.security.internal.admin.port.AdminRolePort;
import com.erp.core.security.internal.admin.web.controller.AdminFieldRuleController;
import com.erp.core.security.internal.admin.web.controller.AdminRoleController;
import com.erp.core.security.internal.admin.web.controller.AdminUserController;
import com.erp.core.security.internal.admin.web.controller.FieldMaskingDemoController;
import com.erp.core.security.internal.authentication.domain.PasswordPolicy;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import com.erp.core.security.internal.authentication.port.PasswordHashingPort;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataRoleAssignmentRepository;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataRolePermissionRepository;
import com.erp.core.security.internal.authorization.application.FieldMaskingService;
import com.erp.core.security.internal.authorization.port.FieldRuleRepositoryPort;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.UUID;

@AutoConfiguration(after = com.erp.core.security.internal.platform.config.SecurityModuleAutoConfiguration.class)
@ConditionalOnProperty(name = "erp.security.enabled", matchIfMissing = true)
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminModuleAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AdminModuleAutoConfiguration.class);

    // ---- User admin ----
    @Bean @ConditionalOnMissingBean
    public AdminUserService adminUserService(CredentialRepositoryPort repo,
                                             PasswordHashingPort hasher,
                                             PasswordPolicy policy) {
        return new AdminUserService(repo, hasher, policy);
    }

    @Bean @ConditionalOnMissingBean
    public AdminUserController adminUserController(AdminUserService service) {
        return new AdminUserController(service);
    }

    // ---- Role port ----
    @Bean @ConditionalOnMissingBean(AdminRolePort.class)
    public AdminRolePort adminRolePort(
            ObjectProvider<SpringDataAdminRoleRepository> roleRepo,
            ObjectProvider<SpringDataRolePermissionRepository> rolePermRepo,
            ObjectProvider<SpringDataRoleAssignmentRepository> roleAssignRepo) {
        SpringDataAdminRoleRepository r = roleRepo.getIfAvailable();
        SpringDataRolePermissionRepository rp = rolePermRepo.getIfAvailable();
        SpringDataRoleAssignmentRepository ra = roleAssignRepo.getIfAvailable();
        if (r != null && rp != null && ra != null) {
            log.info("AdminRolePort: using JPA");
            return new JpaAdminRoleAdapter(r, rp, ra);
        }
        log.warn("AdminRolePort: JPA repos missing (role={}, rolePerm={}, roleAssign={}) -> noop",
                r != null, rp != null, ra != null);
        return new AdminRolePort() {
            @Override public UUID ensureRole(UUID t, String n, int h) { return UUID.randomUUID(); }
            @Override public UUID createRole(UUID t, String n, int h) {
                throw new IllegalStateException("Admin role management requires JPA persistence");
            }
            @Override public void grantPermission(UUID r, String p) {}
            @Override public void revokePermission(UUID r, String p) {}
            @Override public void ensureAssignment(UUID a, UUID r, UUID t) {}
            @Override public void assignRole(UUID a, UUID r, UUID t) {}
            @Override public List<RoleView> listRoles(UUID t) { return List.of(); }
        };
    }

    @Bean @ConditionalOnMissingBean
    public AdminRoleService adminRoleService(AdminRolePort port) {
        return new AdminRoleService(port);
    }

    @Bean @ConditionalOnMissingBean
    public AdminRoleController adminRoleController(AdminRoleService service) {
        return new AdminRoleController(service);
    }

    // ---- Field rule admin (phase 10D) ----
    @Bean @ConditionalOnMissingBean
    public AdminFieldRuleService adminFieldRuleService(FieldRuleRepositoryPort fieldRuleRepo,
                                                       PermissionRepositoryPort permissionRepo) {
        return new AdminFieldRuleService(fieldRuleRepo, permissionRepo);
    }

    @Bean @ConditionalOnMissingBean
    public AdminFieldRuleController adminFieldRuleController(AdminFieldRuleService service) {
        return new AdminFieldRuleController(service);
    }

    @Bean @ConditionalOnMissingBean
    public FieldMaskingDemoController fieldMaskingDemoController(
            CurrentActorProvider actorProvider,
            AuthorizationPort authorizationPort,
            FieldMaskingService maskingService) {
        return new FieldMaskingDemoController(actorProvider, authorizationPort, maskingService);
    }

    // ---- Bootstrap ----
    @Bean @ConditionalOnMissingBean
    public AdminBootstrapRunner adminBootstrapRunner(AdminBootstrapProperties props,
                                                     CredentialRepositoryPort credentialRepo,
                                                     PasswordHashingPort hasher,
                                                     PasswordPolicy policy,
                                                     PermissionRepositoryPort permissionRepo,
                                                     AdminRolePort rolePort) {
        return new AdminBootstrapRunner(props, credentialRepo, hasher, policy, permissionRepo, rolePort);
    }
}