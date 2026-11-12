package com.erp.core.security.internal.authentication.adapter.in.springsecurity;

import com.erp.core.security.internal.authentication.application.AuthenticationCommand;
import com.erp.core.security.internal.authentication.application.AuthenticationOrchestrator;
import com.erp.core.security.internal.authentication.application.AuthenticationResult;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.RoleRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Bridges Spring Security's AuthenticationManager to ErpCore's
 * AuthenticationOrchestrator.
 *
 * SECURITY:
 * - Failure exception is ALWAYS BadCredentialsException (prevents enumeration).
 * - Credentials are erased on the returned token after successful auth.
 *
 * AUTHORIZATION:
 * - On successful authentication, the user's EFFECTIVE permissions
 *   (union of role-based + direct user permissions) are loaded into the
 *   principal's authorities. This enables:
 *     * @PreAuthorize("hasAuthority('...')") checks
 *     * Fast-path authorization without DB round-trips per check
 *     * Permissions encoded into issued JWTs for API clients
 * - If permission resolution fails (DB issue), login still succeeds with
 *   only ROLE_USER. Subsequent @PreAuthorize checks fail safe (deny).
 */
public class ErpAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(ErpAuthenticationProvider.class);
    private static final String DEFAULT_AUTHORITY = "ROLE_USER";

    private final AuthenticationOrchestrator orchestrator;
    private final RoleRepositoryPort roleRepository;

    /** Legacy constructor (unit tests): skips permission resolution. */
    public ErpAuthenticationProvider(AuthenticationOrchestrator orchestrator) {
        this(orchestrator, null);
    }

    /** Production constructor: loads permissions into authorities. */
    public ErpAuthenticationProvider(AuthenticationOrchestrator orchestrator,
                                     RoleRepositoryPort roleRepository) {
        this.orchestrator = orchestrator;
        this.roleRepository = roleRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof ErpAuthenticationToken token)) {
            return null; // not our token type; let another provider handle
        }

        String password = (String) token.getCredentials();
        AuthenticationCommand command = new AuthenticationCommand(
                token.tenantId(), token.username(), password);

        AuthenticationResult result = orchestrator.authenticate(command);

        if (result instanceof AuthenticationResult.Success success) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(DEFAULT_AUTHORITY));

            if (roleRepository != null) {
                try {
                    Set<PermissionKey> permissions = roleRepository.resolvePermissions(
                            success.actorId(), success.tenantId());
                    for (PermissionKey permission : permissions) {
                        authorities.add(new SimpleGrantedAuthority(permission.value()));
                    }
                    log.info("Loaded {} permission authorities for actor={}",
                            permissions.size(), success.actorId());
                } catch (RuntimeException ex) {
                    // Login still succeeds; @PreAuthorize denies protected endpoints.
                    log.warn("Failed to resolve permissions for actor={}: {}",
                            success.actorId(), ex.getMessage(), ex);
                }
            }

            ErpUserDetails principal = new ErpUserDetails(
                    success.actorId(),
                    success.tenantId(),
                    success.principal(),
                    authorities);

            ErpAuthenticationToken authenticated = new ErpAuthenticationToken(
                    principal, principal.getAuthorities());
            authenticated.eraseCredentials();
            return authenticated;
        }

        // Uniform exception — prevents enumeration via exception subtype.
        throw new BadCredentialsException("Authentication failed");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ErpAuthenticationToken.class.isAssignableFrom(authentication);
    }
}