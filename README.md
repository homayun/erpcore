# ErpCore Security Module

> Multi-tenant authentication and authorization for Spring Boot ERP applications.

![Java](https://img.shields.io/badge/Java-25-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-green)
![Tests](https://img.shields.io/badge/Tests-117%20unit%20%2B%205%20IT-brightgreen)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

Most Spring Boot security libraries stop at "can this user log in?".
This module answers the questions that real ERP systems actually need:

- Which tenant is the caller? (cross-tenant denial at 3 layers)
- Which operation can they perform? (RBAC + attribute conditions)
- Which fields can they see? (field-level BOLA prevention with masking)
- Is their auth recent enough for this sensitive action? (step-up primitives)
- What did they do? (tamper-evident audit)

## What actually works, verified by tests

| Capability | Endpoint / Component | Test |
|---|---|---|
| Local login (Argon2id) | POST /login | SecurityFilterChainIntegrationTest |
| JWT (HS256, 15 min) | JwtTokenIssuer | JwtRoundTripTest |
| Refresh rotation + reuse detection | POST /auth/refresh | RefreshTokenServiceTest |
| Account lockout after N failures | InMemoryAccountLockoutAdapter | AccountLockoutEnforcementTest |
| Login rate limiting per client IP | LoginRateLimitFilter | LoginRateLimitKeyTest |
| RBAC (role -> permission) | RoleRepositoryPort | AuthorizationServiceTest |
| Attribute conditions (ABAC-light) | AttributeConditionEvaluator | AttributeConditionEvaluatorTest |
| Field-level authorization + masking | FieldAuthorizationService | FieldAuthorizationServiceTest |
| Method security hasPermission | @PreAuthorize on /admin/** | AdminAccessControlTest |
| Admin user CRUD | POST/GET /admin/users | AdminAccessControlTest |
| Admin role CRUD | POST/GET /admin/roles | AdminAccessControlTest |
| Field rule CRUD (runtime BOLA config) | POST/GET/DELETE /admin/field-rules | AdminFieldRuleAndMaskingTest |
| BOLA prevention, live | GET /admin/demo/invoice | AdminFieldRuleAndMaskingTest |
| JWT secret placeholder fails startup | JwtSecretGuard | JwtSecretGuardTest |
| Cross-tenant denial | AuthorizationService | AuthorizationServiceTest |

117 unit tests, 5 integration tests (Testcontainers), 2 architecture tests
(ArchUnit + Spring Modulith).

## Watch BOLA prevention happen (5-minute demo)

Start the app after enabling bootstrap in application.yml:

    erp:
      security:
        admin:
          bootstrap:
            enabled: true
            tenant-id: 11111111-1111-1111-1111-111111111111
            username: admin
            password: AdminPassword1!

Then in a second terminal:

    $t = "11111111-1111-1111-1111-111111111111"
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $csrf = Invoke-RestMethod -Uri "http://localhost:8080/csrf" -WebSession $s
    $login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/login" `
      -WebSession $s -Headers @{"X-CSRF-TOKEN"=$csrf.token} `
      -ContentType "application/json" `
      -Body "{`"tenantId`":`"$t`",`"username`":`"admin`",`"password`":`"AdminPassword1!`"}"
    $h = @{Authorization = "Bearer $($login.accessToken)"}

    # BEFORE: no rule for INVOICE.amount -> amount is a raw number
    Invoke-RestMethod -Uri "http://localhost:8080/admin/demo/invoice" -Headers $h

    # Configure a runtime rule
    Invoke-RestMethod -Method Post -Uri "http://localhost:8080/admin/field-rules" -Headers $h `
      -ContentType "application/json" `
      -Body '{"resourceType":"INVOICE","fieldName":"amount","requiredPermission":"VIEW_INVOICE_AMOUNT","maskingWhenDenied":"REDACT"}'

    # AFTER: same caller, same endpoint -> amount is "***"
    Invoke-RestMethod -Uri "http://localhost:8080/admin/demo/invoice" -Headers $h

No code change. No redeploy. Runtime configuration through a REST endpoint,
and the next request from a caller without VIEW_INVOICE_AMOUNT gets the masked
value. That is field-level BOLA prevention in production shape.

## Architecture

Hexagonal + Clean Architecture, enforced by Spring Modulith and ArchUnit at build time.

    com.erp.core.security
    +-- api/            Public contract - stable, versioned
    |   +-- actor/      CurrentActor, CurrentActorProvider
    |   +-- authorization/  AuthorizationPort, field-level contracts
    |   +-- audit/      SecurityAuditPort
    |   +-- error/      SecurityErrorCode, exceptions
    |   +-- event/      Domain events
    |   +-- permission/ PermissionDeclarationPort
    |   +-- tenant/     TenantScope
    +-- internal/       Implementation - invisible outside the module
        +-- authentication/  Argon2, orchestrator, credentials, lockout
        +-- authorization/   RBAC, ABAC, field rules, method security
        +-- abuse/           Rate limiting, login filter
        +-- audit/           SLF4J sink, recorder
        +-- context/         Tenant context, actor bridge, JWT bridge
        +-- jwt/             Encoder, decoder, secret guard
        +-- refresh/         Token rotation, cleanup
        +-- web/             Filter chains, handlers, /login, /refresh, /csrf
        +-- admin/           User CRUD, role CRUD, field rule CRUD
        +-- platform/        Auto-configuration, health, properties

ArchUnit enforces: api/** may never import internal/**. Verified on every build
by ModuleBoundaryTest. Spring Modulith structure verified by
ApplicationModulesVerificationTest.

## Configuration

All properties under erp.security.*. Full reference in
docs/configuration-reference.md.

    erp:
      security:
        enabled: true
        persistence:
          type: jpa           # jpa (default) | memory
        jwt:
          secret: ${JWT_SECRET}   # 32+ bytes, no default in prod
        redis:
          enabled: false      # true -> distributed lockout + rate limit
        cleanup:
          cron: "0 0 3 * * *" # daily refresh-token purge

Production checklist (see application-prod.yml):

- JWT_SECRET set (JwtSecretGuard refuses startup otherwise)
- spring.datasource.* -> PostgreSQL 16+ (H2 is dev only)
- spring.jpa.hibernate.ddl-auto=validate (Flyway owns the schema)
- server.forward-headers-strategy=native (rate limiting uses remoteAddr)

## Testing

    # Unit + architecture (fast, no Docker)
    mvn clean verify

    # + Integration (Testcontainers - needs Docker)
    mvn verify

## Known limitations

Listed honestly. Each item in docs/SECURITY.md with the attack model and
why it is not yet addressed.

- No HTTP endpoint for step-up auth (core logic in SensitiveOperationGuard exists)
- Disabled accounts: JWT remains valid until its 15-minute expiry
- Rate limiting per client IP only (no per-account exponential backoff)
- No MFA, email verification, or password history
- Refresh tokens have no parallel-request grace window
- Field rule values are strings; typed attributes are a follow-up

## Debugging notes

This module went through 10 non-trivial bugs during development - from
@EnableMethodSecurity silently denying everything to @ConditionalOnBean(DataSource.class)
being evaluated before the DataSource exists. Every root cause and fix is
documented in docs/DEBUGGING.md. Reading it is the fastest way to understand
why the code looks the way it does.

## Requirements

- Java 25 (LTS)
- Spring Boot 4.1.2+
- PostgreSQL 16+ for production, H2 for development
- Redis 7+ optional (distributed lockout / rate limit)
- Docker (only for integration tests)

## License

MIT - see LICENSE.