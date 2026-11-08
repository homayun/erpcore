package com.erp.core.security.internal.authentication.application;

import com.erp.core.security.api.event.AuthenticationFailedEvent;
import com.erp.core.security.api.event.AuthenticationSucceededEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationOrchestratorTest {

    @Mock
    private LocalAuthenticationService localAuthenticationService;

    @Mock
    private ApplicationEventPublisher events;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private final Clock fixedClock =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void publishesSuccessEvent() {
        AuthenticationOrchestrator orchestrator =
                new AuthenticationOrchestrator(localAuthenticationService, events, fixedClock);

        UUID actorId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(localAuthenticationService.authenticate(any()))
                .thenReturn(new AuthenticationResult.Success(actorId, tenantId, "alice"));

        orchestrator.authenticate(new AuthenticationCommand(tenantId, "alice", "AnyPwd1!"));

        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(AuthenticationSucceededEvent.class);
        AuthenticationSucceededEvent event = (AuthenticationSucceededEvent) eventCaptor.getValue();
        assertThat(event.actorId()).isEqualTo(actorId);
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.principal()).isEqualTo("alice");
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void publishesFailureEvent() {
        AuthenticationOrchestrator orchestrator =
                new AuthenticationOrchestrator(localAuthenticationService, events, fixedClock);

        UUID tenantId = UUID.randomUUID();
        when(localAuthenticationService.authenticate(any()))
                .thenReturn(new AuthenticationResult.Failure(
                        com.erp.core.security.api.event.AuthenticationFailureReason.INVALID_CREDENTIALS));

        orchestrator.authenticate(new AuthenticationCommand(tenantId, "ghost", "AnyPwd1!"));

        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(AuthenticationFailedEvent.class);
        AuthenticationFailedEvent event = (AuthenticationFailedEvent) eventCaptor.getValue();
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.principal()).isEqualTo("ghost");
    }

    /**
     * We must verify the OBJECT overload of publishEvent â€” the orchestrator's
     * events are plain records (not ApplicationEvent subclasses), so the
     * compiler resolves the call to publishEvent(Object).
     *
     * Using any(Class) instead of bare any() forces the same overload at verify
     * time. Bare any() would resolve to the more specific publishEvent(ApplicationEvent)
     * overload, which is never invoked, producing a false "Wanted but not invoked".
     */
    @Test
    void doesNotPublishDuplicateEvents() {
        AuthenticationOrchestrator orchestrator =
                new AuthenticationOrchestrator(localAuthenticationService, events, fixedClock);

        UUID actorId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(localAuthenticationService.authenticate(any()))
                .thenReturn(new AuthenticationResult.Success(actorId, tenantId, "alice"));

        orchestrator.authenticate(new AuthenticationCommand(tenantId, "alice", "AnyPwd1!"));

        verify(events, times(1)).publishEvent(any(AuthenticationSucceededEvent.class));
        verify(events, never()).publishEvent(any(AuthenticationFailedEvent.class));
    }
}