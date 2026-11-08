# Security posture

This module is designed to fail closed on authorization decisions. The rest
of this document is an honest accounting of what that means, what is not yet
addressed, and what threat models were considered.

## What "fail closed" means here

- Anonymous actor -> DENY
- Missing attribute in ABAC condition -> DENY
- Non-numeric value where LTE/GTE expected -> DENY (not "pass by accident")
- Role lookup throws (DB down, timeout) -> DENY (permission set treated as empty)
- Cross-tenant resource access -> DENY at three layers
- Unknown field in field-level authorization -> DENY by default
- Missing JWT secret -> application refuses to start (JwtSecretGuard)
- Field rule FK violation -> 500 (database is unhealthy - not a permission check)

## Threat models considered

    Attack                                    Defence                                  Where
    ----------------------------------------  ---------------------------------------  -----------------------------------------
    Username enumeration via timing           Dummy hash compare on unknown user       LocalAuthenticationService
    Username enumeration via exception type   All failures throw BadCredentialsException  ErpAuthenticationProvider
    Login CSRF                                CSRF token required on POST /login       BrowserSecurityFilterChainConfiguration
    Login brute-force per IP                  Token bucket rate limit                  LoginRateLimitFilter
    Account lockout evasion via IP rotation   Per-account lockout                      InMemoryAccountLockoutAdapter
    XFF spoofing for rate-limit evasion       Keys on request.getRemoteAddr()          LoginRateLimitFilter
    Refresh token theft                       Rotation + family-based reuse detection  RefreshTokenService
    Token reuse after logout                  Family revocation on reuse signal        RefreshTokenService
    Cross-tenant data access (IDOR/BOLA)      actor.tenantId == resource.tenantId      AuthorizationService
    Field-level BOLA (mass-assignment)        Declarative field rules + masking        FieldAuthorizationService
    Numeric ABAC bypass (amount <= "abc")     NumberFormatException -> deny            AttributeConditionEvaluator
    Stale authentication for sensitive ops    SensitiveOperationGuard                  (core logic; endpoint pending)
    Unauthenticated actuator leaks            Only /actuator/health and /info exposed  SecurityHeadersConfiguration
    Default JWT secret in production          Startup fails unless explicitly overridden  JwtSecretGuard

## Known limitations

Each is a conscious trade-off, not a bug. Ordered roughly by severity.

### High

1. Disabled accounts keep their JWT until expiry.
   The /api/** and /admin/** chains are stateless. When an admin disables a user,
   that user's JWT remains valid until its 15-minute expiry (accessTtl). The
   refresh endpoint re-checks account status, so after at most 15 minutes the
   account cannot refresh. Mitigation options: (a) reduce accessTtl to 5 minutes
   for admin-heavy deployments, (b) add a Redis-backed revocation list checked by
   the JWT filter. Neither is implemented.

2. No step-up authentication endpoint.
   SensitiveOperationGuard and AuthenticationFreshnessEvaluator are complete and
   tested. There is no POST /auth/step-up endpoint yet. A consumer that wants
   step-up must currently check AuthenticationFreshnessEvaluator themselves and
   re-authenticate via /login if stale.

3. Rate limiting is per client IP only.
   No per-username, no per-account exponential backoff. A distributed attack
   from many IPs against one account is only caught by account lockout after N
   failures (default 5). Lockout is per-account and survives IP changes, so a
   targeted attack is bounded, but the POST /login path itself is still hot.

### Medium

4. Refresh tokens have no grace window.
   Parallel requests presenting the same refresh token both fail on the second
   one (reuse detection fires). Real browsers can trigger this during tab
   storms. Grace window: not implemented.

5. No MFA.
   No TOTP, no WebAuthn. AuthenticationOrchestrator is shaped to support it
   (add a strategy step before returning Success), but no implementation exists.

6. No email verification, password history, or password expiry.
   PasswordPolicy validates strength (length, character classes). Nothing more.

7. Field rule values are strings.
   AttributeCondition.value is a String; numeric comparisons parse to BigDecimal.
   If a consumer wants structured values (JSON, dates), they must serialize and
   compare as strings. Typed attributes are a follow-up.

8. Audit sink is SLF4J, not durable storage.
   Slf4jSecurityAuditAdapter writes to a structured log. A consumer that needs
   durable, tamper-evident audit records must supply a different SecurityAuditPort
   implementation (DB, event bus, etc.). The interface supports this; no default
   implementation does.

### Low

9. No password reset flow.
   No "forgot password" endpoint. Consumers must implement this themselves.

10. No session invalidation across nodes without Redis.
    InMemoryAccountLockoutAdapter and InMemoryRateLimitAdapter are per-JVM. In
    a multi-node deployment, enable erp.security.redis.enabled=true to switch
    to distributed implementations.

11. CORS is not configured.
    Consumers must configure CORS themselves. The module does not assume a
    frontend origin.

## What is deliberately NOT claimed

- No "100% secure" claim
- No claim of OWASP ASVS certification
- No claim of penetration-test validation
- No claim that these defenses are exhaustive

The module is a well-structured implementation of standard practices. Every
defense listed above has a corresponding test. Every limitation listed above
is real.

## Reporting

If you find a security issue, please open a private security advisory on
GitHub rather than a public issue.