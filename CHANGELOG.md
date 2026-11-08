# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed (security)

- Account lockout is now enforced (locked accounts rejected, failures recorded)
- Login rate limit keys on the real remote address; X-Forwarded-For is no longer trusted
- JWT callers on /api/** get their authorities, actor and tenant mapped
- Refresh keeps the real username and re-checks the account (new `enabled` flag, V5)
- Startup fails when the JWT secret is missing or the placeholder value
- `ddl-auto=update` is dev-only; `application-prod.yml` validates schema via Flyway

## [0.1.0] - 2026-11-08

First public release. Covers Phases 01 through 09b of the architecture
blueprint.

### Added

- **Authentication**
  - Local username/password login with Argon2id hashing (BouncyCastle 1.86)
  - Login rate limiting per client IP (Bucket4j token bucket)
  - Account lockout after configurable consecutive failures
  - Authentication success/failure domain events

- **Session and Token**
  - Server-side session-based login for browser clients (CSRF protected)
  - JWT bearer access tokens for API clients (HS256, 15 min default)
  - Opaque refresh tokens with SHA-256 hash storage
  - Refresh token rotation with family-based reuse detection
  - Scheduled refresh-token cleanup (expired + stale revoked)
  - Session rotation on login (session-fixation prevention)

- **Authorization**
  - RBAC engine with permission catalog and role assignment
  - Attribute-based conditions (ABAC-light): EQUALS, IN, LTE, GTE, CONTAINS_ANY
  - Field-level authorization with declarative rules in `sec_field_rule`
  - Field masking strategies: FULL, PARTIAL, HASH, REDACT
  - Method-level security via `@PreAuthorize` + custom `hasPermission(...)`
  - Cross-tenant denial at three layers (filter, service, RLS-ready)

- **Step-up authentication**
  - `SensitiveOperationGuard` upgrades permit to `STEP_UP_REQUIRED` when
    the permission is marked sensitive and auth is stale
  - `AuthenticationFreshnessEvaluator` with fail-closed clock-skew handling
  - Fresh auth cannot upgrade a DENY - only permits step-up on PERMIT

- **Abuse protection**
  - Distributed rate limiting (Redis) or in-memory (single node)
  - Distributed account lockout (Redis) or in-memory (single node)
  - Configurable thresholds and durations

- **Audit and compliance**
  - Security audit events with configurable SLF4J sink
  - Sanitizer strips secrets from audit payloads
  - Health indicator (actuator-compatible)

- **Persistence**
  - Flyway migrations V1 through V4
  - JPA persistence for credentials, refresh tokens, permissions, roles,
    attribute conditions, field rules
  - In-memory fallback for dev/test
  - JPA and memory adapters behind hexagonal ports

- **Architecture**
  - Spring Modulith + ArchUnit build-time verification
  - Hexagonal API boundary (`api` vs `internal`) enforced by test
  - 101 unit tests, 5 integration tests (Testcontainers), 2 arch tests

### Known limitations

- Flyway auto-configuration on Spring Boot 4.x requires explicit dependency;
  see `docs/CONFIGURATION.md` for details
- Refresh token rotation has no grace window for parallel requests
- Step-up endpoint (HTTP) not yet wired; core logic is complete
- Permission catalog bootstrap requires a consumer-supplied
  `ApplicationRunner` that calls `PermissionDeclarationPort`

### Security

- All authorization paths fail closed
- No hardcoded production secrets; default JWT secret emits startup warning
- Adversarial tests cover: IDOR, BOLA, mass assignment, non-numeric
  attribute values, stale auth, cross-tenant access, refresh token reuse

[Unreleased]: https://github.com/your-org/erpcore/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/your-org/erpcore/releases/tag/v0.1.0