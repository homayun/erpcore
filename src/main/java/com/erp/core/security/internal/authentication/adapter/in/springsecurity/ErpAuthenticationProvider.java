package com.erp.core.security.internal.authentication.adapter.in.springsecurity;

import com.erp.core.security.internal.authentication.application.AuthenticationCommand;
import com.erp.core.security.internal.authentication.application.AuthenticationOrchestrator;
import com.erp.core.security.internal.authentication.application.AuthenticationResult;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Bridges Spring Security's AuthenticationManager to ErpCore's
 * AuthenticationOrchestrator.
 *
 * SECURITY:
 * - The failure exception is ALWAYS BadCredentialsException, never a more
 *   specific exception. This prevents username enumeration via exception type.
 * - Credentials are erased on the returned token after successful auth.
 */
public class ErpAuthenticationProvider implements AuthenticationProvider {

    private static final String DEFAULT_AUTHORITY = "ROLE_USER";

    private final AuthenticationOrchestrator orchestrator;

    public ErpAuthenticationProvider(AuthenticationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof ErpAuthenticationToken token)) {
            return null;  // not our token type; let another provider handle
        }

        String password = (String) token.getCredentials();
        AuthenticationCommand command = new AuthenticationCommand(
                token.tenantId(), token.username(), password);

        AuthenticationResult result = orchestrator.authenticate(command);

        if (result instanceof AuthenticationResult.Success success) {
            ErpUserDetails principal = new ErpUserDetails(
                    success.actorId(),
                    success.tenantId(),
                    success.principal(),
                    List.of(new SimpleGrantedAuthority(DEFAULT_AUTHORITY)));
            ErpAuthenticationToken authenticated = new ErpAuthenticationToken(
                    principal, principal.getAuthorities());
            authenticated.eraseCredentials();
            return authenticated;
        }

        // Uniform exception â€” prevents enumeration via exception subtype.
        throw new BadCredentialsException("Authentication failed");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ErpAuthenticationToken.class.isAssignableFrom(authentication);
    }
}