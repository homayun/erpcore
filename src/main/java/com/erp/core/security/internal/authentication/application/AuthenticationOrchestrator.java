package com.erp.core.security.internal.authentication.application;

import com.erp.core.security.api.event.AuthenticationFailedEvent;
import com.erp.core.security.api.event.AuthenticationSucceededEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;

/**
 * Central entry point for authentication. Currently only delegates to
 * LocalAuthenticationService, but the shape supports adding OIDC, MFA, and
 * step-up strategies without changing callers.
 *
 * Publishes domain events AFTER the underlying service returns. Events never
 * contain passwords, hashes, or tokens.
 */
public class AuthenticationOrchestrator {

    private final LocalAuthenticationService localAuthenticationService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AuthenticationOrchestrator(
            LocalAuthenticationService localAuthenticationService,
            ApplicationEventPublisher events,
            Clock clock
    ) {
        this.localAuthenticationService = localAuthenticationService;
        this.events = events;
        this.clock = clock;
    }

    public AuthenticationResult authenticate(AuthenticationCommand command) {
        AuthenticationResult result = localAuthenticationService.authenticate(command);
        Instant now = clock.instant();

        if (result instanceof AuthenticationResult.Success success) {
            events.publishEvent(new AuthenticationSucceededEvent(
                    success.actorId(),
                    success.tenantId(),
                    success.principal(),
                    now
            ));
        } else if (result instanceof AuthenticationResult.Failure failure) {
            events.publishEvent(new AuthenticationFailedEvent(
                    command.tenantId(),
                    command.username(),
                    failure.reason(),
                    now
            ));
        }

        return result;
    }
}