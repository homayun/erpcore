# ErpCore Security Module â€” Configuration Reference

All properties are under the `erp.security.*` prefix. Defaults are production-safe unless noted.

## Module toggle

| Property | Type | Default | Description |
|---|---|---|---|
| `erp.security.enabled` | boolean | `true` | Master switch. When `false`, no filters, controllers, or auto-wired beans are registered. |

## Persistence

| Property | Type | Default | Description |
|---|---|---|---|
| `erp.security.persistence.type` | string | `jpa` | Backend for credential and refresh-token storage. `jpa` = Spring Data JPA (requires a `DataSource`). `memory` = in-memory adapters (dev/tests only). |

When `jpa` is selected but no `DataSource` bean exists, the module falls back to `memory` adapters. The fallback is never silent in production â€” check `SecurityModuleHealthIndicator`.

## JWT

| Property | Type | Default | Description |
|---|---|---|---|
| `erp.security.jwt.issuer` | string | `erp-core` | `iss` claim. Should match your deployment's canonical issuer URL in production. |
| `erp.security.jwt.secret` | string | *(dev default)* | HS256 signing key, at least 32 bytes. **Override in production.** |
| `erp.security.jwt.access-ttl` | Duration | `PT15M` | Access token lifetime. |
| `erp.security.jwt.clock-skew` | Duration | `PT30S` | Tolerated clock skew during validation. |

## Refresh tokens

| Property | Type | Default | Description |
|---|---|---|---|
| `erp.security.refresh-token.ttl` | Duration | `P30D` | Refresh token lifetime. |
| `erp.security.refresh-token.cookie-name` | string | `erp_refresh` | Cookie name for browser clients. |

## Account lockout

| Property | Type | Default | Description |
|---|---|---|---|
| `erp.security.lockout.max-failed-attempts` | int | `5` | Consecutive failures before lockout. |
| `erp.security.lockout.lock-duration` | Duration | `PT15M` | Lockout duration. |

## Cleanup job

| Property | Type | Default | Description |
|---|---|---|---|
| `erp.security.cleanup.enabled` | boolean | `true` | Enable the scheduled refresh-token cleanup. Requires `spring-boot-starter-actuator`? No â€” only `@EnableScheduling` which the module provides automatically. |
| `erp.security.cleanup.cron` | string | `0 0 3 * * *` | Cron expression (daily at 03:00). |
| `erp.security.cleanup.retention` | Duration | `P7D` | How long to keep revoked tokens for reuse detection. |

## Redis (opt-in)

| Property | Type | Default | Description |
|---|---|---|---|
| `erp.security.redis.enabled` | boolean | `false` | When `true`, distributed rate limiting and lockout via Redis. When `false`, in-memory (single-node only). |

## Storage / external

| Property | Description |
|---|---|
| `spring.datasource.*` | Standard Spring datasource. Required for `persistence.type=jpa`. |
| `spring.data.redis.*` | Standard Spring Redis. Required for `erp.security.redis.enabled=true`. |
| `spring.flyway.*` | Standard Flyway. Migrations live under `classpath:db/migration`. |

## Actuator (optional)

Add `spring-boot-starter-actuator` to expose `/actuator/health`. The module contributes:

- `securityModule` â€” module status, persistence type, Redis opt-in, and a warning if the default JWT secret is still in use.

Never exposes secrets, tokens, or credentials.