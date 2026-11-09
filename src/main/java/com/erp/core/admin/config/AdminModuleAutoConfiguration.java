package com.erp.core.admin.config;

import com.erp.core.admin.adapter.out.jpa.JpaAdminRoleAdapter;
import com.erp.core.admin.adapter.out.jpa.SpringDataAdminRoleRepository;
import com.erp.core.admin.application.AdminBootstrapRunner;
import com.erp.core.admin.application.AdminFieldRuleService;
import com.erp.core.admin.application.AdminRoleService;
import com.erp.core.admin.application.AdminPermissionService;
import com.erp.core.admin.application.AdminUserService;
import com.erp.core.admin.internal.company.adapter.out.jpa.BranchRepository;
import com.erp.core.admin.internal.company.adapter.out.jpa.CompanyRepository;
import com.erp.core.admin.internal.company.adapter.out.jpa.MasterCompanyRepository;
import com.erp.core.admin.internal.company.application.BranchService;
import com.erp.core.admin.internal.company.application.CompanyService;
import com.erp.core.admin.internal.company.application.MasterCompanyService;
import com.erp.core.admin.internal.setting.adapter.out.jpa.CodeSettingRepository;
import com.erp.core.admin.internal.setting.application.CodeSettingService;
import com.erp.core.admin.port.AdminRolePort;
import com.erp.core.admin.web.BranchWebController;
import com.erp.core.admin.web.CodeSettingWebController;
import com.erp.core.admin.web.CompanyWebController;
import com.erp.core.admin.web.LoginViewController;
import com.erp.core.admin.web.MasterCompanyWebController;
import com.erp.core.admin.web.PermissionWebController;
import com.erp.core.admin.web.RoleWebController;
import com.erp.core.admin.web.UserWebController;
import com.erp.core.admin.web.controller.AdminFieldRuleController;
import com.erp.core.admin.web.controller.AdminRoleController;
import com.erp.core.admin.web.controller.AdminUserController;
import com.erp.core.admin.web.controller.FieldMaskingDemoController;
import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.api.authorization.AuthorizationPort;
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
import org.springframework.context.annotation.Import;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@AutoConfiguration(after = com.erp.core.security.internal.platform.config.SecurityModuleAutoConfiguration.class)
@ConditionalOnProperty(name = "erp.security.enabled", matchIfMissing = true)
@EnableConfigurationProperties(AdminBootstrapProperties.class)
@Import(LoginViewController.class)
public class AdminModuleAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AdminModuleAutoConfiguration.class);

    // ==================================================
    // Company / Master Company / Branch
    // ==================================================

    @Bean @ConditionalOnMissingBean
    public CompanyService companyService(CompanyRepository companyRepo, BranchRepository branchRepo, MasterCompanyRepository masterRepo) {
        return new CompanyService(companyRepo, branchRepo, masterRepo);
    }

    @Bean @ConditionalOnMissingBean
    public MasterCompanyService masterCompanyService(MasterCompanyRepository masterRepo, CompanyRepository companyRepo) {
        return new MasterCompanyService(masterRepo, companyRepo);
    }

    @Bean @ConditionalOnMissingBean
    public BranchService branchService(BranchRepository branchRepo, CompanyRepository companyRepo) {
        return new BranchService(branchRepo, companyRepo);
    }

    @Bean @ConditionalOnMissingBean
    public CompanyWebController companyWebController(CompanyService companyService) {
        return new CompanyWebController(companyService);
    }

    @Bean @ConditionalOnMissingBean
    public MasterCompanyWebController masterCompanyWebController(MasterCompanyService masterCompanyService) {
        return new MasterCompanyWebController(masterCompanyService);
    }

    @Bean @ConditionalOnMissingBean
    public BranchWebController branchWebController(BranchService branchService) {
        return new BranchWebController(branchService);
    }

    // ==================================================
    // Code Setting
    // ==================================================

    @Bean @ConditionalOnMissingBean
    public CodeSettingService codeSettingService(CodeSettingRepository repo) {
        return new CodeSettingService(repo);
    }

    @Bean @ConditionalOnMissingBean
    public CodeSettingWebController codeSettingWebController(CodeSettingService service) {
        return new CodeSettingWebController(service);
    }

    // ==================================================
    // Permission
    // ==================================================

    @Bean @ConditionalOnMissingBean
    public AdminPermissionService adminPermissionService(com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataPermissionRepository repo) {
        return new AdminPermissionService(repo);
    }

    @Bean @ConditionalOnMissingBean
    public PermissionWebController permissionWebController(AdminPermissionService service) {
        return new PermissionWebController(service);
    }

    // ==================================================
    // User Admin
    // ==================================================

    @Bean @ConditionalOnMissingBean
    public AdminUserService adminUserService(CredentialRepositoryPort repo,
                                             PasswordHashingPort hasher,
                                             PasswordPolicy policy,
                                             SpringDataAdminRoleRepository roleRepo,
                                             SpringDataRoleAssignmentRepository roleAssignmentRepo,
                                             com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataUserPermissionRepository userPermRepo) {
        return new AdminUserService(repo, hasher, policy, roleRepo, roleAssignmentRepo, userPermRepo);
    }

    @Bean @ConditionalOnMissingBean
    public AdminUserController adminUserController(AdminUserService service) {
        return new AdminUserController(service);
    }

    @Bean @ConditionalOnMissingBean
    public UserWebController userWebController(AdminUserService service,
                                                PermissionRepositoryPort permissionRepo) {
        return new UserWebController(service, permissionRepo);
    }

    // ==================================================
    // Role Admin
    // ==================================================

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
        log.warn("AdminRolePort: JPA repos missing -> noop fallback");
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
            @Override public void updateRole(UUID r, String n, int h) {}
            @Override public void deleteRole(UUID r) {}
            @Override public Set<String> getAssignedPermissions(UUID r) { return Set.of(); }
            @Override public void replacePermissions(UUID r, Collection<String> keys) {}
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

    @Bean @ConditionalOnMissingBean
    public RoleWebController roleWebController(AdminRoleService service,
                                                SpringDataAdminRoleRepository roleRepo,
                                                PermissionRepositoryPort permissionRepo) {
        return new RoleWebController(service, roleRepo, permissionRepo);
    }

    // ==================================================
    // Field Rule / Field Masking
    // ==================================================

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

    // ==================================================
    // Bootstrap
    // ==================================================

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