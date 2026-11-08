# Debugging notes

Ten bugs that surfaced during development, in the order they appeared.
Each entry: what went wrong, why it was hard to see, what the fix was.
This document exists because every one of these is a story about a silent
failure - the kind of bug that costs hours in production if you don't know
the shape.

## Bug 1 - @EnableMethodSecurity silently denies every hasPermission(...)

Symptom: @PreAuthorize("hasPermission(...)") returns 403 for every request,
even for admins with the right permission.

Root cause: MethodSecurityExpressionHandler was registered as a non-static @Bean.
@EnableMethodSecurity imports an internal PrePostMethodSecurityConfiguration that
uses @Autowired(required = false) MethodSecurityExpressionHandler to pick up a
custom handler. Non-static beans are created after the configuration class is
instantiated, by which time the internal config has already captured the default
handler (which has no PermissionEvaluator). Default handler silently denies every
hasPermission call.

Fix: Register MethodSecurityExpressionHandler as a static @Bean:

    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionEvaluator permissionEvaluator) { ... }

Lesson: @EnableMethodSecurity + hasPermission(...) is the most quietly broken
wiring in Spring Security 6+. If @PreAuthorize denies everything, this is the
first thing to check.

## Bug 2 - @ConditionalOnBean(DataSource.class) evaluates before DataSource exists

Symptom: Application starts. /admin/users returns 403. Logs show no errors.
Every JPA repository port is silently a no-op fallback.

Root cause: Inside an @AutoConfiguration class, @ConditionalOnBean(DataSource.class)
evaluates during the auto-configuration phase, which runs BEFORE
DataSourceAutoConfiguration has finished registering the DataSource bean.
Condition fails, JPA adapter is skipped, no-op fallback wins silently.

Fix: Replace @ConditionalOnBean(X.class) with ObjectProvider<X> injection and
check getIfAvailable() at bean-construction time:

    @Bean @ConditionalOnMissingBean(CredentialRepositoryPort.class)
    public CredentialRepositoryPort credentialRepositoryPort(
            ObjectProvider<SpringDataCredentialRepository> repoProvider) {
        SpringDataCredentialRepository repo = repoProvider.getIfAvailable();
        if (repo != null) return new JpaCredentialRepositoryAdapter(repo);
        return new InMemoryCredentialRepositoryAdapter();
    }

Lesson: Every one of the 7 repository ports in this module had this bug.
Applying the same fix to all of them was necessary - fixing only one just
moves the failure.

## Bug 3 - @RestController missing on controller class

Symptom: POST /admin/users returns 404 with NoResourceFoundException, handler
shows ResourceHttpRequestHandler instead of the controller.

Root cause: The controller class had @RequestMapping("/admin/users") but was
registered as a @Bean, not with @RestController. RequestMappingHandlerMapping
only treats classes with @Controller (or @RestController) as handlers. A bean
with only @RequestMapping is not picked up.

Fix: Add @RestController (or @Controller) to every handler class.

Lesson: Bean presence does not imply handler mapping. The 404 with
NoResourceFoundException is the tell.

## Bug 4 - CSRF on /login (and why that is correct)

Symptom: POST /login returns 403 from SecurityFilterChainIntegrationTest.

Root cause: Spring Security 7 enables CSRF by default on the browser filter
chain. Test did not include .with(csrf()).

Fix: Add .with(csrf()) to test, and real clients must fetch GET /csrf first.

Lesson - why we did NOT disable CSRF on login: Login CSRF is a real attack where
an attacker forces the victim to log in as the attacker, then sniffs their
activity. Disabling CSRF on /login "for convenience" reintroduces that attack.
Machine clients must fetch /csrf first, exactly like a browser. That is the
correct shape of the API - not a bug to work around.

## Bug 5 - PowerShell Get-Content -Raw corrupted UTF-8 YAML

Symptom: "unacceptable code point '?' (0x9D) special characters are not allowed".

Root cause: A PowerShell script read application.yml with Get-Content -Raw,
which on Windows PowerShell 5.1 uses the system codepage, not UTF-8. The file
contained an em-dash (UTF-8 E2 80 94). PowerShell decoded it as three codepage
bytes and re-encoded to UTF-8 as E2 3F 3F. YAML parser rejects the resulting
0x3F 0x3F sequence.

Fix: Never use Get-Content -Raw on UTF-8 files in Windows PowerShell 5.1. Use:

    [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
    [System.IO.File]::WriteAllText($path, $content, (New-Object System.Text.UTF8Encoding $false))

And keep source files ASCII-only when possible.

## Bug 6 - sec_field_rule.required_permission FK violation

Symptom: POST /admin/field-rules returns 500. H2 log:
Referential integrity constraint violation: FK_FIELD_RULE_PERMISSION.

Root cause: The admin field-rule service created a rule referencing
VIEW_INVOICE_AMOUNT without that permission existing in sec_permission.

Fix: Auto-register the permission idempotently inside the admin service:

    if (!permissionRepository.exists(permissionKey)) {
        permissionRepository.upsertAll(List.of(new PermissionDefinition(...)));
    }

Why not remove the FK: The FK is a catalog-integrity guard. Removing it would
let typos silently corrupt the catalog. Auto-register keeps the FK as a second
line of defence - if the insert still fails, the database is genuinely
unhealthy and 500 is the correct response.

## Bug 7 - PasswordPolicyViolation returns 500 instead of 400

Symptom: Creating a user with a too-short password returns 500.

Root cause: PasswordPolicyViolation extends RuntimeException, not
IllegalArgumentException. The controller caught IllegalArgumentException |
IllegalStateException, so the violation escaped to the generic 500 handler.

Fix: Catch PasswordPolicyViolation explicitly and return 400 with structured
violations:

    catch (PasswordPolicyViolation ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", "SEC-4001",
                "message", "Password does not satisfy policy",
                "violations", ex.getViolations()));
    }

Lesson: Exceptions are part of the API contract. Any exception that reaches
the client with the wrong status code is a bug in the exception hierarchy,
not a bug in the caller.

## Bug 8 - FieldRuleRepositoryPort not a functional interface after CRUD added

Symptom: Compilation error - FieldRuleRepositoryPort is not a functional
interface - multiple non-overriding abstract methods found.

Root cause: The port originally had one method (findByResourceType), so tests
used it as a lambda. When CRUD was added (findAll, upsert, delete, exists),
existing tests broke.

Fix: Converted test lambdas to anonymous classes. Long-term, the port should
be split into FieldRuleQueryPort (read side, used by FieldAuthorizationService)
and FieldRuleCommandPort (write side, used by AdminFieldRuleService). That
refactor is deferred - the immediate fix preserves behavior.

Lesson: Interface Segregation Principle exists for a reason. A query-side
service should not be forced to implement write-side methods just because
they share an entity.

## Bug 9 - Emoji in PowerShell breaks .ps1 parsing

Symptom: "Unexpected token 'tests' in expression or statement" in a .ps1 file.

Root cause: The file contained an emoji and unescaped parentheses inside a
double-quoted Write-Host string. Windows PowerShell 5.1 parses .ps1 files using
the system codepage, and a multi-byte UTF-8 emoji corrupts the surrounding
tokens.

Fix: Every .ps1 file in this repo is now ASCII-only. No emoji, no curly quotes,
no em-dashes in scripts.

Lesson: Same family as Bug 5. If you write scripts for a Windows environment
that might land on a machine with a different codepage, keep them ASCII.

## Bug 10 - Bootstrap "succeeded" but wrote nothing

Symptom: AdminBootstrapRunner printed "Created bootstrap admin user: admin",
but login returned 401 and role resolution returned [].

Root cause: The no-op fallback AdminRolePort was injected because Bug 2 was
present. Every method of the no-op adapter silently did nothing, including
grantPermission and ensureAssignment. Bootstrap "ran to completion" without
any exception.

Fix: Same fix as Bug 2 (ObjectProvider pattern). Also added explicit step logs
(Bootstrap step 1 done, Bootstrap step 2 done, ...) so that if any step is
skipped silently in the future, the log makes it visible.

Lesson: Silent fallbacks are the most dangerous pattern in security code. A
no-op implementation that "always succeeds" is worse than a bean that fails
to load, because nothing in the log distinguishes "worked" from "did nothing".