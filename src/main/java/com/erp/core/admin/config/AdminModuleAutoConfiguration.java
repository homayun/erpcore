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
    public CompanyWebController companyWebController(CompanyService companyService,
                                                         CurrentActorProvider actorProvider) {
        return new CompanyWebController(companyService, actorProvider);
    }

    @Bean @ConditionalOnMissingBean
    public MasterCompanyWebController masterCompanyWebController(MasterCompanyService masterCompanyService,
                                                              CurrentActorProvider actorProvider) {
        return new MasterCompanyWebController(masterCompanyService, actorProvider);
    }

    @Bean @ConditionalOnMissingBean
    public BranchWebController branchWebController(BranchService branchService,
                                                       CurrentActorProvider actorProvider) {
        return new BranchWebController(branchService, actorProvider);
    }

    // ==================================================
    // Code Setting
    // ==================================================

    @Bean @ConditionalOnMissingBean
    public CodeSettingService codeSettingService(CodeSettingRepository repo) {
        return new CodeSettingService(repo);
    }

    @Bean @ConditionalOnMissingBean
    public CodeSettingWebController codeSettingWebController(CodeSettingService service,
                                                        CurrentActorProvider actorProvider) {
        return new CodeSettingWebController(service, actorProvider);
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
                                             com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataUserPermissionRepository userPermRepo,
                                             com.erp.core.security.internal.tenant.port.TenantMembershipPort membership) {
        return new AdminUserService(repo, hasher, policy, roleRepo, roleAssignmentRepo, userPermRepo, membership);
    }

    @Bean @ConditionalOnMissingBean
    public AdminUserController adminUserController(AdminUserService service) {
        return new AdminUserController(service);
    }

    @Bean @ConditionalOnMissingBean
    public UserWebController userWebController(AdminUserService service,
                                                PermissionRepositoryPort permissionRepo,
                                                CurrentActorProvider actorProvider) {
        return new UserWebController(service, permissionRepo, actorProvider);
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
            @Override public UUID ensureRole(UUID tenantId, String name, int hierarchyLevel) {
                throw new UnsupportedOperationException("No AdminRolePort adapter configured");
            }
            @Override public UUID createRole(UUID tenantId, String name, int hierarchyLevel) {
                throw new UnsupportedOperationException("No AdminRolePort adapter configured");
            }
            @Override public void grantPermission(UUID roleId, String permissionKey) {
                throw new UnsupportedOperationException("No AdminRolePort adapter configured");
            }
            @Override public void revokePermission(UUID roleId, String permissionKey) {
                throw new UnsupportedOperationException("No AdminRolePort adapter configured");
            }
            @Override public void ensureAssignment(UUID actorId, UUID roleId, UUID tenantId) {
                throw new UnsupportedOperationException("No AdminRolePort adapter configured");
            }
            @Override public void assignRole(UUID actorId, UUID roleId, UUID tenantId) {
                throw new UnsupportedOperationException("No AdminRolePort adapter configured");
            }
            @Override public List<RoleView> listRoles(UUID tenantId) {
                return java.util.Collections.emptyList();
            }
            @Override public void updateRole(UUID roleId, String newName, int hierarchyLevel) {
                throw new UnsupportedOperationException("No AdminRolePort adapter configured");
            }
            @Override public void deleteRole(UUID roleId) {
                throw new UnsupportedOperationException("No AdminRolePort adapter configured");
            }
            @Override public void replacePermissions(UUID roleId, java.util.List<String> permissions) {
                throw new UnsupportedOperationException("No AdminRolePort adapter configured");
            }
            @Override public java.util.Set<String> getPermissionsForRole(UUID roleId) {
                return java.util.Collections.emptySet();
            }
        };
    }

    @Bean @ConditionalOnMissingBean
    public AdminRoleService adminRoleService(AdminRolePort port, PermissionRepositoryPort permissionRepo) {
        return new AdminRoleService(port, permissionRepo);
    }

    @Bean @ConditionalOnMissingBean
    public AdminRoleController adminRoleController(AdminRoleService service) {
        return new AdminRoleController(service);
    }

    @Bean @ConditionalOnMissingBean
    public RoleWebController roleWebController(AdminRoleService service,
                                                SpringDataAdminRoleRepository roleRepo,
                                                PermissionRepositoryPort permissionRepo,
                                                CurrentActorProvider actorProvider) {
        return new RoleWebController(service, roleRepo, permissionRepo, actorProvider);
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

    // ==================================================
    // Tenant management
    // ==================================================

    @Bean @ConditionalOnMissingBean
    public com.erp.core.admin.tenant.application.TenantService tenantService(
            com.erp.core.admin.tenant.adapter.out.jpa.SpringDataTenantRepository tenantRepo,
            com.erp.core.security.internal.tenant.port.TenantMembershipPort membership,
            CredentialRepositoryPort credentialRepo,
            PasswordHashingPort hasher,
            PasswordPolicy policy,
            SpringDataAdminRoleRepository roleRepo,
            SpringDataRolePermissionRepository rolePermissionRepo,
            SpringDataRoleAssignmentRepository roleAssignmentRepo) {
        return new com.erp.core.admin.tenant.application.TenantService(
                tenantRepo, membership, credentialRepo, hasher, policy,
                roleRepo, rolePermissionRepo, roleAssignmentRepo);
    }

    @Bean @ConditionalOnMissingBean
    public com.erp.core.admin.web.TenantWebController tenantWebController(
            com.erp.core.admin.tenant.application.TenantService service,
            CurrentActorProvider actorProvider) {
        return new com.erp.core.admin.web.TenantWebController(service, actorProvider);
    }

    // ==================================================
    // Bootstrap
    // ==================================================



    // ==================================================
    // Division filter (MasterCompany-centric UI)
    // ==================================================

    @Bean @ConditionalOnMissingBean
    public com.erp.core.admin.web.DivisionSwitchController divisionSwitchController(
            CurrentActorProvider actorProvider,
            MasterCompanyRepository masterCompanyRepo) {
        return new com.erp.core.admin.web.DivisionSwitchController(actorProvider, masterCompanyRepo);
    }

    @Bean @ConditionalOnMissingBean
    public com.erp.core.admin.web.DivisionModelAdvice divisionModelAdvice(
            CurrentActorProvider actorProvider,
            MasterCompanyRepository masterCompanyRepo) {
        return new com.erp.core.admin.web.DivisionModelAdvice(actorProvider, masterCompanyRepo);
    }
@Bean @ConditionalOnMissingBean
    public AdminBootstrapRunner adminBootstrapRunner(AdminBootstrapProperties props,
                                                     CredentialRepositoryPort credentialRepo,
                                                     PasswordHashingPort hasher,
                                                     PasswordPolicy policy,
                                                     PermissionRepositoryPort permissionRepo,
                                                     AdminRolePort rolePort,
                                                     com.erp.core.security.internal.tenant.port.TenantMembershipPort membership) {
        return new AdminBootstrapRunner(props, credentialRepo, hasher, policy, permissionRepo, rolePort, membership);
    }
}