# Architecture

## Layers

This module follows Hexagonal Architecture. There is exactly one public contract
(api), and everything else (internal) is implementation.

    +-----------------------------+
    |  Consumer module            |
    |  (finance, inventory,       |
    |   tenant admin, ...)        |
    +--------------+--------------+
                   | imports ONLY
                   v
    +-----------------------------+
    |  security.api               |   public contract
    |  CurrentActorProvider       |
    |  AuthorizationPort          |
    |  PermissionDeclarationPort  |
    |  SecurityAuditPort          |
    +--------------+--------------+
                   | implemented by
                   v
    +-----------------------------+
    |  security.internal          |   implementation
    |  adapters (in / out)        |
    |  application services       |
    |  domain                     |
    +-----------------------------+

Enforcement:

- ModuleBoundaryTest (ArchUnit) - api may not import internal
- ApplicationModulesVerificationTest (Spring Modulith) - module structure verified

## Inside internal

Clean Architecture layering, per feature:

    internal.authorization/
    +-- adapter/
    |   +-- in/springsecurity/   ErpPermissionEvaluator (hasPermission SpEL)
    |   +-- out/jpa/             JPA entities + Spring Data repositories
    +-- application/             AuthorizationService, FieldAuthorizationService,
    |                            PermissionRegistryService, SensitiveOperationGuard
    +-- domain/                  PermissionKey, PermissionDefinition,
    |                            AttributeCondition, FieldRule (pure records)
    +-- port/                    PermissionRepositoryPort, RoleRepositoryPort,
                                 FieldRuleRepositoryPort, AttributeConditionRepositoryPort

Every adapter/out/* implements a port/* interface. application/* depends only
on ports. domain/* depends on nothing. This is why 117 unit tests run without
a Spring context.

## Request flow - JWT on /admin/** or /api/**

    1. BearerTokenAuthenticationFilter
       - JwtDecoder validates: signature, iss, typ, exp, nbf, clock skew
       - Identity claim validator requires tenant_id AND actor_id as UUIDs

    2. TenantContextRestorationFilter
       - Binds tenant_id to ThreadLocal (TenantContextHolder)

    3. MethodSecurityInterceptor (@PreAuthorize)
       - Parses hasPermission('admin', 'ADMIN_USER', 'MANAGE')
       - Calls ErpPermissionEvaluator.hasPermission(...)
           - CurrentActorProvider.getCurrentActor() -> SpringSecurityActorBridge
           - Builds AuthorizationRequest
           - AuthorizationService.authorize(...)
               - Cross-tenant check (actor.tenantId == resource.tenantId)
               - Resolve effective permissions
                   - Actor authorities
                   - DB: role assignments -> role permissions
               - Attribute conditions (ABAC)
               - Returns PERMIT / DENY / STEP_UP_REQUIRED

    4. Controller method executes (only if PERMIT)

    5. TenantContextRestorationFilter clears ThreadLocal in finally

## Request flow - browser session on /login

    1. CSRF filter (Spring Security 7 default)
       - Requires X-CSRF-TOKEN header for POST /login
       - Prevents login CSRF (attacker logs victim in as attacker)

    2. LoginRateLimitFilter
       - Per-client-IP token bucket (Bucket4j)

    3. LoginController
       - AuthenticationManager -> ErpAuthenticationProvider
           - AuthenticationOrchestrator -> LocalAuthenticationService
               - Argon2id verify (constant-time)
               - Account lockout check
               - Dummy hash compare on unknown user (timing defence)

    4. Response: JWT + refresh token (HttpOnly, Secure, SameSite=Strict cookie)

## Persistence

Flyway migrations under src/main/resources/db/migration/. Hibernate
ddl-auto=validate in production (Flyway owns the schema).

    Table                        Purpose                       Notable columns
    ---------------------------  ----------------------------  ---------------------
    sec_credentials              User credentials              (tenant_id, username) unique
    sec_refresh_token            Opaque refresh tokens         SHA-256 hashed, family rotation
    sec_permission               Permission catalog            Auto-registered if referenced
    sec_role                     Roles per tenant              hierarchy_level reserved
    sec_role_permission          Role -> permission grants     FK to sec_permission
    sec_role_assignment          User -> role                  FK to sec_role
    sec_attribute_condition      ABAC conditions               FK to sec_permission
    sec_field_rule               Per-field auth rules          FK to sec_permission

## Auto-configuration ordering

SecurityModuleAutoConfiguration uses ObjectProvider<X> for every repository port
instead of @ConditionalOnBean(X.class). This is deliberate - see
docs/DEBUGGING.md "Bug 2". Summary: @ConditionalOnBean(DataSource.class)
evaluates before DataSourceAutoConfiguration finishes registering the DataSource,
so every JPA port silently falls back to a no-op. ObjectProvider.getIfAvailable()
runs at bean construction time, after everything is registered.

## Method security

MethodSecurityConfiguration registers MethodSecurityExpressionHandler as a
static @Bean. This is also deliberate - see docs/DEBUGGING.md "Bug 1".
A non-static @Bean is created after @EnableMethodSecurity's internal configuration
captures the default handler, silently denying every hasPermission(...).