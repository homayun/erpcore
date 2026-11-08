package com.erp.core.security.internal.platform.config;

import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.api.audit.SecurityAuditPort;
import com.erp.core.security.api.authorization.AuthorizationPort;
import com.erp.core.security.api.permission.PermissionDeclarationPort;
import com.erp.core.security.internal.abuse.adapter.out.memory.InMemoryRateLimitAdapter;
import com.erp.core.security.internal.abuse.adapter.out.redis.RedisRateLimitAdapter;
import com.erp.core.security.internal.abuse.filter.LoginRateLimitFilter;
import com.erp.core.security.internal.abuse.port.RateLimitPort;
import com.erp.core.security.internal.audit.adapter.out.slf4j.Slf4jSecurityAuditAdapter;
import com.erp.core.security.internal.audit.application.SecurityAuditRecorder;
import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpAuthenticationProvider;
import com.erp.core.security.internal.authentication.adapter.out.hashing.Argon2PasswordHasher;
import com.erp.core.security.internal.authentication.adapter.out.jdbc.JdbcCredentialStatusAdapter;
import com.erp.core.security.internal.authentication.adapter.out.jpa.JpaCredentialRepositoryAdapter;
import com.erp.core.security.internal.authentication.adapter.out.jpa.SpringDataCredentialRepository;
import com.erp.core.security.internal.authentication.adapter.out.memory.InMemoryAccountLockoutAdapter;
import com.erp.core.security.internal.authentication.adapter.out.memory.InMemoryCredentialRepositoryAdapter;
import com.erp.core.security.internal.authentication.adapter.out.redis.RedisAccountLockoutAdapter;
import com.erp.core.security.internal.authentication.application.AuthenticationOrchestrator;
import com.erp.core.security.internal.authentication.application.LocalAuthenticationService;
import com.erp.core.security.internal.authentication.domain.PasswordPolicy;
import com.erp.core.security.internal.authentication.port.AccountLockoutPort;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import com.erp.core.security.internal.authentication.port.CredentialStatusPort;
import com.erp.core.security.internal.authentication.port.PasswordHashingPort;
import com.erp.core.security.internal.authorization.adapter.in.springsecurity.ErpPermissionEvaluator;
import com.erp.core.security.internal.authorization.adapter.out.jpa.JpaAttributeConditionRepositoryAdapter;
import com.erp.core.security.internal.authorization.adapter.out.jpa.JpaFieldRuleRepositoryAdapter;
import com.erp.core.security.internal.authorization.adapter.out.jpa.JpaPermissionRepositoryAdapter;
import com.erp.core.security.internal.authorization.adapter.out.jpa.JpaRoleRepositoryAdapter;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataAttributeConditionRepository;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataFieldRuleRepository;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataPermissionRepository;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataRolePermissionRepository;
import com.erp.core.security.internal.authorization.application.AttributeConditionEvaluator;
import com.erp.core.security.internal.authorization.application.AuthenticationFreshnessEvaluator;
import com.erp.core.security.internal.authorization.application.AuthorizationService;
import com.erp.core.security.internal.authorization.application.FieldAuthorizationService;
import com.erp.core.security.internal.authorization.application.FieldMaskingService;
import com.erp.core.security.internal.authorization.application.PermissionRegistryService;
import com.erp.core.security.internal.authorization.application.SensitiveOperationGuard;
import com.erp.core.security.internal.authorization.config.MethodSecurityConfiguration;
import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.AttributeConditionRepositoryPort;
import com.erp.core.security.internal.authorization.port.FieldRuleRepositoryPort;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import com.erp.core.security.internal.authorization.port.RoleRepositoryPort;
import com.erp.core.security.internal.context.application.capture.SecurityContextCaptureService;
import com.erp.core.security.internal.context.application.capture.SecurityContextSnapshotFactory;
import com.erp.core.security.internal.context.infrastructure.springsecurity.SpringSecurityActorBridge;
import com.erp.core.security.internal.jwt.application.JwtTokenIssuer;
import com.erp.core.security.internal.jwt.config.JwtConfiguration;
import com.erp.core.security.internal.jwt.config.JwtSecretGuard;
import com.erp.core.security.internal.refresh.adapter.out.jpa.JpaRefreshTokenRepositoryAdapter;
import com.erp.core.security.internal.refresh.adapter.out.jpa.SpringDataRefreshTokenRepository;
import com.erp.core.security.internal.refresh.adapter.out.memory.InMemoryRefreshTokenRepositoryAdapter;
import com.erp.core.security.internal.refresh.application.RefreshTokenCleanupJob;
import com.erp.core.security.internal.refresh.application.RefreshTokenService;
import com.erp.core.security.internal.refresh.port.RefreshTokenRepositoryPort;
import com.erp.core.security.internal.web.config.ApiSecurityFilterChainConfiguration;
import com.erp.core.security.internal.web.config.BrowserSecurityFilterChainConfiguration;
import com.erp.core.security.internal.web.controller.CsrfController;
import com.erp.core.security.internal.web.controller.LoginController;
import com.erp.core.security.internal.web.controller.RefreshController;
import com.erp.core.security.internal.web.handler.JsonAccessDeniedHandler;
import com.erp.core.security.internal.web.handler.JsonAuthenticationEntryPoint;
import com.erp.core.security.internal.web.handler.JsonLogoutSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@AutoConfiguration(before = org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class)
@EnableConfigurationProperties(SecurityProperties.class)
@Import(MethodSecurityConfiguration.class)
public class SecurityModuleAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityModuleAutoConfiguration.class);

    // =================================================================
    // Context
    // =================================================================

    @Bean @ConditionalOnMissingBean
    public CurrentActorProvider currentActorProvider() { return new SpringSecurityActorBridge(); }
    @Bean @ConditionalOnMissingBean
    public SecurityContextSnapshotFactory securityContextSnapshotFactory() { return new SecurityContextSnapshotFactory(); }
    @Bean @ConditionalOnMissingBean
    public SecurityContextCaptureService securityContextCaptureService(SecurityContextSnapshotFactory f) {
        return new SecurityContextCaptureService(f);
    }

    // =================================================================
    // Password
    // =================================================================

    @Bean @ConditionalOnMissingBean
    public PasswordHashingPort passwordHashingPort() { return Argon2PasswordHasher.productionDefaults(); }
    @Bean @ConditionalOnMissingBean
    public PasswordPolicy passwordPolicy() { return PasswordPolicy.defaults(); }

    // =================================================================
    // Credential repository - ObjectProvider (avoids @ConditionalOnBean ordering bug)
    // =================================================================

    @Bean @ConditionalOnMissingBean(CredentialRepositoryPort.class)
    @ConditionalOnProperty(name = "erp.security.persistence.type", havingValue = "jpa", matchIfMissing = true)
    public CredentialRepositoryPort credentialRepositoryPort(
            ObjectProvider<SpringDataCredentialRepository> repoProvider) {
        SpringDataCredentialRepository repo = repoProvider.getIfAvailable();
        if (repo != null) {
            log.info("CredentialRepositoryPort: using JPA");
            return new JpaCredentialRepositoryAdapter(repo);
        }
        log.info("CredentialRepositoryPort: SpringDataCredentialRepository not available -> memory fallback");
        return new InMemoryCredentialRepositoryAdapter();
    }

    @Bean @ConditionalOnMissingBean(CredentialRepositoryPort.class)
    public CredentialRepositoryPort memoryCredentialRepository() {
        log.info("CredentialRepositoryPort: explicit memory bean");
        return new InMemoryCredentialRepositoryAdapter();
    }

    @Bean @ConditionalOnMissingBean
    public LocalAuthenticationService localAuthenticationService(CredentialRepositoryPort repo,
                                                                 PasswordHashingPort hasher,
                                                                 AccountLockoutPort lockoutPort) {
        return new LocalAuthenticationService(repo, hasher, lockoutPort);
    }

    // =================================================================
    // Credential status - ObjectProvider
    // =================================================================

    @Bean @ConditionalOnMissingBean(CredentialStatusPort.class)
    public CredentialStatusPort credentialStatusPort(ObjectProvider<DataSource> dsProvider) {
        DataSource ds = dsProvider.getIfAvailable();
        if (ds != null) {
            log.info("CredentialStatusPort: using JDBC");
            return new JdbcCredentialStatusAdapter(ds);
        }
        log.info("CredentialStatusPort: no DataSource -> memory stub");
        return (actorId, tenantId) -> java.util.Optional.of(
                new CredentialStatusPort.CredentialStatus("user", true));
    }

    @Bean @ConditionalOnMissingBean
    public JwtSecretGuard jwtSecretGuard(SecurityProperties props, Environment env) {
        return new JwtSecretGuard(props, env);
    }

    @Bean @ConditionalOnMissingBean
    public AuthenticationOrchestrator authenticationOrchestrator(LocalAuthenticationService local,
                                                                 ApplicationEventPublisher events) {
        return new AuthenticationOrchestrator(local, events, Clock.systemUTC());
    }

    // =================================================================
    // Account lockout
    // =================================================================

    @Bean @ConditionalOnMissingBean(AccountLockoutPort.class)
    @ConditionalOnProperty(name = "erp.security.redis.enabled", havingValue = "true")
    public AccountLockoutPort redisAccountLockout(StringRedisTemplate redis, SecurityProperties props) {
        return new RedisAccountLockoutAdapter(redis, props);
    }
    @Bean @ConditionalOnMissingBean(AccountLockoutPort.class)
    public AccountLockoutPort inMemoryAccountLockout(SecurityProperties props) {
        return new InMemoryAccountLockoutAdapter(props, Clock.systemUTC());
    }

    // =================================================================
    // Rate limit
    // =================================================================

    @Bean @ConditionalOnMissingBean(RateLimitPort.class)
    @ConditionalOnProperty(name = "erp.security.redis.enabled", havingValue = "true")
    public RateLimitPort redisRateLimit(io.github.bucket4j.distributed.proxy.ProxyManager<byte[]> pm) {
        return new RedisRateLimitAdapter(pm, 10, Duration.ofMinutes(15));
    }
    @Bean @ConditionalOnMissingBean(RateLimitPort.class)
    public RateLimitPort inMemoryRateLimit() { return new InMemoryRateLimitAdapter(10, Duration.ofMinutes(15)); }
    @Bean @ConditionalOnMissingBean
    public LoginRateLimitFilter loginRateLimitFilter(RateLimitPort port, SecurityProperties props) {
        return new LoginRateLimitFilter(port, props);
    }

    // =================================================================
    // JWT
    // =================================================================

    @Bean @ConditionalOnMissingBean
    public JwtEncoder jwtEncoder(SecurityProperties props) { return JwtConfiguration.jwtEncoder(props); }
    @Bean @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(SecurityProperties props) { return JwtConfiguration.jwtDecoder(props); }
    @Bean @ConditionalOnMissingBean
    public JwtTokenIssuer jwtTokenIssuer(JwtEncoder encoder, SecurityProperties props) {
        return new JwtTokenIssuer(encoder, props, Clock.systemUTC());
    }

    // =================================================================
    // Refresh token repository - ObjectProvider
    // =================================================================

    @Bean @ConditionalOnMissingBean(RefreshTokenRepositoryPort.class)
    @ConditionalOnProperty(name = "erp.security.persistence.type", havingValue = "jpa", matchIfMissing = true)
    public RefreshTokenRepositoryPort refreshTokenRepositoryPort(
            ObjectProvider<SpringDataRefreshTokenRepository> repoProvider) {
        SpringDataRefreshTokenRepository repo = repoProvider.getIfAvailable();
        if (repo != null) {
            log.info("RefreshTokenRepositoryPort: using JPA");
            return new JpaRefreshTokenRepositoryAdapter(repo);
        }
        log.info("RefreshTokenRepositoryPort: SpringDataRefreshTokenRepository not available -> memory");
        return new InMemoryRefreshTokenRepositoryAdapter();
    }
    @Bean @ConditionalOnMissingBean(RefreshTokenRepositoryPort.class)
    public RefreshTokenRepositoryPort memoryRefreshTokenRepository() {
        log.info("RefreshTokenRepositoryPort: explicit memory bean");
        return new InMemoryRefreshTokenRepositoryAdapter();
    }
    @Bean @ConditionalOnMissingBean
    public RefreshTokenService refreshTokenService(RefreshTokenRepositoryPort repo, SecurityProperties props) {
        return new RefreshTokenService(repo, props, Clock.systemUTC());
    }

    // =================================================================
    // Audit
    // =================================================================

    @Bean @ConditionalOnMissingBean
    public SecurityAuditPort securityAuditPort() { return new Slf4jSecurityAuditAdapter(); }
    @Bean @ConditionalOnMissingBean
    public SecurityAuditRecorder securityAuditRecorder(SecurityAuditPort port) {
        return new SecurityAuditRecorder(port, Clock.systemUTC());
    }
    @Bean @ConditionalOnMissingBean
    public DefaultAuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher p) {
        return new DefaultAuthenticationEventPublisher(p);
    }

    // =================================================================
    // Spring Security integration
    // =================================================================

    @Bean @ConditionalOnMissingBean
    public ErpAuthenticationProvider erpAuthenticationProvider(AuthenticationOrchestrator o) {
        return new ErpAuthenticationProvider(o);
    }
    @Bean @ConditionalOnMissingBean
    public SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean @ConditionalOnMissingBean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() { return new ChangeSessionIdAuthenticationStrategy(); }
    @Bean @ConditionalOnMissingBean
    public JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint(ObjectMapper om) { return new JsonAuthenticationEntryPoint(om); }
    @Bean @ConditionalOnMissingBean
    public JsonAccessDeniedHandler jsonAccessDeniedHandler(ObjectMapper om) { return new JsonAccessDeniedHandler(om); }
    @Bean @ConditionalOnMissingBean
    public JsonLogoutSuccessHandler jsonLogoutSuccessHandler(ObjectMapper om) { return new JsonLogoutSuccessHandler(om); }
    @Bean @ConditionalOnMissingBean
    public AuthenticationManager authenticationManager(ErpAuthenticationProvider p) { return new ProviderManager(p); }

    // =================================================================
    // Authorization - ObjectProvider for every JPA port
    // =================================================================

    @Bean @ConditionalOnMissingBean
    public AttributeConditionEvaluator attributeConditionEvaluator() { return new AttributeConditionEvaluator(); }

    @Bean @ConditionalOnMissingBean(PermissionRepositoryPort.class)
    @ConditionalOnProperty(name = "erp.security.persistence.type", havingValue = "jpa", matchIfMissing = true)
    public PermissionRepositoryPort permissionRepositoryPort(
            ObjectProvider<SpringDataPermissionRepository> repoProvider) {
        SpringDataPermissionRepository repo = repoProvider.getIfAvailable();
        if (repo != null) {
            log.info("PermissionRepositoryPort: using JPA");
            return new JpaPermissionRepositoryAdapter(repo);
        }
        log.info("PermissionRepositoryPort: SpringDataPermissionRepository not available -> noop");
        return noopPermissionRepo();
    }
    @Bean @ConditionalOnMissingBean(PermissionRepositoryPort.class)
    public PermissionRepositoryPort noopPermissionRepository() {
        log.info("PermissionRepositoryPort: explicit noop bean");
        return noopPermissionRepo();
    }
    private static PermissionRepositoryPort noopPermissionRepo() {
        return new PermissionRepositoryPort() {
            @Override public void upsertAll(Collection<PermissionDefinition> definitions) {}
            @Override public boolean exists(PermissionKey key) { return false; }
            @Override public List<PermissionDefinition> findAll() { return List.of(); }
        };
    }

    @Bean @ConditionalOnMissingBean(RoleRepositoryPort.class)
    @ConditionalOnProperty(name = "erp.security.persistence.type", havingValue = "jpa", matchIfMissing = true)
    public RoleRepositoryPort roleRepositoryPort(
            ObjectProvider<SpringDataRolePermissionRepository> repoProvider) {
        SpringDataRolePermissionRepository repo = repoProvider.getIfAvailable();
        if (repo != null) {
            log.info("RoleRepositoryPort: using JPA");
            return new JpaRoleRepositoryAdapter(repo);
        }
        log.info("RoleRepositoryPort: SpringDataRolePermissionRepository not available -> noop");
        return (actorId, tenantId) -> Set.of();
    }
    @Bean @ConditionalOnMissingBean(RoleRepositoryPort.class)
    public RoleRepositoryPort noopRoleRepository() {
        log.info("RoleRepositoryPort: explicit noop bean");
        return (actorId, tenantId) -> Set.of();
    }

    @Bean @ConditionalOnMissingBean(AttributeConditionRepositoryPort.class)
    @ConditionalOnProperty(name = "erp.security.persistence.type", havingValue = "jpa", matchIfMissing = true)
    public AttributeConditionRepositoryPort attributeConditionRepositoryPort(
            ObjectProvider<SpringDataAttributeConditionRepository> repoProvider) {
        SpringDataAttributeConditionRepository repo = repoProvider.getIfAvailable();
        if (repo != null) {
            log.info("AttributeConditionRepositoryPort: using JPA");
            return new JpaAttributeConditionRepositoryAdapter(repo);
        }
        log.info("AttributeConditionRepositoryPort: SpringDataAttributeConditionRepository not available -> noop");
        return key -> List.of();
    }
    @Bean @ConditionalOnMissingBean(AttributeConditionRepositoryPort.class)
    public AttributeConditionRepositoryPort noopAttributeConditionRepository() {
        log.info("AttributeConditionRepositoryPort: explicit noop bean");
        return key -> List.of();
    }

    @Bean @ConditionalOnMissingBean(FieldRuleRepositoryPort.class)
    @ConditionalOnProperty(name = "erp.security.persistence.type", havingValue = "jpa", matchIfMissing = true)
    public FieldRuleRepositoryPort fieldRuleRepositoryPort(
            ObjectProvider<SpringDataFieldRuleRepository> repoProvider) {
        SpringDataFieldRuleRepository repo = repoProvider.getIfAvailable();
        if (repo != null) {
            log.info("FieldRuleRepositoryPort: using JPA");
            return new JpaFieldRuleRepositoryAdapter(repo);
        }
        log.info("FieldRuleRepositoryPort: SpringDataFieldRuleRepository not available -> noop");
        return new FieldRuleRepositoryPort() {
            @Override public List<com.erp.core.security.internal.authorization.domain.FieldRule> findByResourceType(String rt) { return List.of(); }
            @Override public List<com.erp.core.security.internal.authorization.domain.FieldRule> findAll() { return List.of(); }
            @Override public com.erp.core.security.internal.authorization.domain.FieldRule upsert(com.erp.core.security.internal.authorization.domain.FieldRule rule) { return rule; }
            @Override public boolean delete(String rt, String fn) { return false; }
            @Override public boolean exists(String rt, String fn) { return false; }
        };
    }
    @Bean @ConditionalOnMissingBean(FieldRuleRepositoryPort.class)
    public FieldRuleRepositoryPort noopFieldRuleRepository() {
        log.info("FieldRuleRepositoryPort: explicit noop bean");
        return new FieldRuleRepositoryPort() {
            @Override public List<com.erp.core.security.internal.authorization.domain.FieldRule> findByResourceType(String rt) { return List.of(); }
            @Override public List<com.erp.core.security.internal.authorization.domain.FieldRule> findAll() { return List.of(); }
            @Override public com.erp.core.security.internal.authorization.domain.FieldRule upsert(com.erp.core.security.internal.authorization.domain.FieldRule rule) { return rule; }
            @Override public boolean delete(String rt, String fn) { return false; }
            @Override public boolean exists(String rt, String fn) { return false; }
        };
    }

    @Bean @ConditionalOnMissingBean
    public FieldAuthorizationService fieldAuthorizationService(FieldRuleRepositoryPort fieldRepo,
                                                               RoleRepositoryPort roleRepository) {
        return new FieldAuthorizationService(fieldRepo, roleRepository);
    }

    @Bean @ConditionalOnMissingBean
    public AuthorizationPort authorizationPort(PermissionRepositoryPort permissionRepository,
                                               RoleRepositoryPort roleRepository,
                                               AttributeConditionRepositoryPort conditionRepository,
                                               AttributeConditionEvaluator evaluator,
                                               FieldAuthorizationService fieldService) {
        return new AuthorizationService(permissionRepository, roleRepository, conditionRepository, evaluator, fieldService);
    }

    @Bean @ConditionalOnMissingBean
    public PermissionDeclarationPort permissionDeclarationPort(PermissionRepositoryPort repo) {
        return new PermissionRegistryService(repo);
    }

    @Bean @ConditionalOnMissingBean
    public ErpPermissionEvaluator erpPermissionEvaluator(AuthorizationPort authorizationPort,
                                                         CurrentActorProvider actorProvider) {
        return new ErpPermissionEvaluator(authorizationPort, actorProvider);
    }

    @Bean @ConditionalOnMissingBean
    public AuthenticationFreshnessEvaluator authenticationFreshnessEvaluator() {
        return new AuthenticationFreshnessEvaluator(Clock.systemUTC(), Duration.ofMinutes(15));
    }

    @Bean @ConditionalOnMissingBean
    public SensitiveOperationGuard sensitiveOperationGuard(AuthorizationPort authorizationPort,
                                                           PermissionRepositoryPort permissionRepository,
                                                           AuthenticationFreshnessEvaluator freshnessEvaluator) {
        return new SensitiveOperationGuard(authorizationPort, permissionRepository, freshnessEvaluator);
    }

    // =================================================================
    // Filter chains
    // =================================================================

    @Bean @Order(1) @ConditionalOnMissingBean(name = "apiSecurityFilterChain")
    @ConditionalOnProperty(name = "erp.security.enabled", matchIfMissing = true)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      JsonAuthenticationEntryPoint ep,
                                                      JsonAccessDeniedHandler dh,
                                                      JwtDecoder jd) throws Exception {
        return ApiSecurityFilterChainConfiguration.build(http, ep, dh, jd);
    }

    @Bean @Order(2) @ConditionalOnMissingBean(name = "browserSecurityFilterChain")
    @ConditionalOnProperty(name = "erp.security.enabled", matchIfMissing = true)
    public SecurityFilterChain browserSecurityFilterChain(HttpSecurity http,
                                                          SecurityProperties p,
                                                          ErpAuthenticationProvider pr,
                                                          JsonAuthenticationEntryPoint ep,
                                                          JsonAccessDeniedHandler dh,
                                                          JsonLogoutSuccessHandler lh,
                                                          LoginRateLimitFilter rlf) throws Exception {
        return BrowserSecurityFilterChainConfiguration.build(http, p, pr, ep, dh, lh, rlf);
    }

    // =================================================================
    // Endpoints
    // =================================================================

    @Bean @ConditionalOnProperty(name = "erp.security.enabled", matchIfMissing = true)
    public LoginController loginController(AuthenticationManager am, JwtTokenIssuer ji,
                                           RefreshTokenService rs, AccountLockoutPort lp,
                                           CredentialStatusPort csp, SecurityProperties p) {
        return new LoginController(am, ji, rs, lp, p, csp);
    }
    @Bean @ConditionalOnProperty(name = "erp.security.enabled", matchIfMissing = true)
    public RefreshController refreshController(RefreshTokenService rs, JwtTokenIssuer ji,
                                               SecurityProperties p, CredentialStatusPort csp) {
        return new RefreshController(rs, ji, p, csp);
    }
    @Bean @ConditionalOnProperty(name = "erp.security.enabled", matchIfMissing = true)
    public CsrfController csrfController() { return new CsrfController(); }

    // =================================================================
    // =================================================================
    // Field masking (phase 10D)
    // =================================================================

    @Bean @ConditionalOnMissingBean
    public FieldMaskingService fieldMaskingService() {
        return new FieldMaskingService();
    }

    // Scheduled cleanup
    // =================================================================

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({DataSource.class, SpringDataRefreshTokenRepository.class})
    @ConditionalOnProperty(name = "erp.security.cleanup.enabled", matchIfMissing = true)
    @ConditionalOnProperty(name = "erp.security.persistence.type", havingValue = "jpa", matchIfMissing = true)
    @EnableScheduling
    static class RefreshTokenCleanupConfiguration {
        @Bean @ConditionalOnMissingBean
        public RefreshTokenCleanupJob refreshTokenCleanupJob(SpringDataRefreshTokenRepository repo,
                                                             SecurityProperties props) {
            return new RefreshTokenCleanupJob(repo, props, Clock.systemUTC());
        }
    }
}