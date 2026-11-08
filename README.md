# ErpCore Security Module

[![CI](https://github.com/homayun/erpcore/actions/workflows/ci.yml/badge.svg)](https://github.com/homayun/erpcore/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-green)
![Spring Modulith](https://img.shields.io/badge/Spring%20Modulith-2.1.1-purple)
![Tests](https://img.shields.io/badge/Tests-117%20unit%20%2B%205%20IT-brightgreen)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

### 🚀 [Try the Live Demo](https://erp-quickstart-demo.onrender.com)
> **Credentials:** Tenant: `11111111-1111-1111-1111-111111111111` | Username: `admin` | Password: `AdminPassword1!`

> Multi-tenant Spring Boot 4 security module: RBAC + ABAC, field-level BOLA prevention with masking, JWT + refresh rotation with reuse detection, Argon2id, and tamper-evident audit. Hexagonal + Spring Modulith, 117 verified tests.

Most Spring Boot security libraries stop at "can this user log in?".
This module answers the questions that real ERP systems actually need:

- **Which tenant** is the caller? (cross-tenant denial at 3 layers)
- **Which operation** can they perform? (RBAC + attribute conditions)
- **Which fields** can they see? (field-level BOLA prevention with masking)
- **Is their auth recent enough** for this sensitive action? (step-up primitives)
- **What did they do?** (tamper-evident audit)

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

Start the app after enabling bootstrap in `application.yml`:

```yaml
erp:
  security:
    admin:
      bootstrap:
        enabled: true
        tenant-id: 11111111-1111-1111-1111-111111111111
        username: admin
        password: AdminPassword1!