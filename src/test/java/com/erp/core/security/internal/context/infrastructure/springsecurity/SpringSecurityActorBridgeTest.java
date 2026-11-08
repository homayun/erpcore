package com.erp.core.security.internal.context.infrastructure.springsecurity;

import com.erp.core.security.api.actor.ActorType;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpUserDetails;
import com.erp.core.security.internal.data.application.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SpringSecurityActorBridgeTest {

    private final SpringSecurityActorBridge bridge = new SpringSecurityActorBridge();

    @BeforeEach
    void reset() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void noAuthenticationReturnsAnonymous() {
        CurrentActor actor = bridge.getCurrentActor();
        assertThat(actor.type()).isEqualTo(ActorType.ANONYMOUS);
        assertThat(actor.isAuthenticated()).isFalse();
    }

    @Test
    void anonymousAuthenticationReturnsAnonymous() {
        var auth = new AnonymousAuthenticationToken(
                "k", "anon", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(bridge.getCurrentActor().type()).isEqualTo(ActorType.ANONYMOUS);
    }

    @Test
    void erpUserDetailsIsResolvedCompletely() {
        UUID actorId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ErpUserDetails user = new ErpUserDetails(actorId, tenantId, "alice",
                Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        CurrentActor actor = bridge.getCurrentActor();

        assertThat(actor.type()).isEqualTo(ActorType.USER);
        assertThat(actor.id()).isEqualTo(actorId);
        assertThat(actor.tenantId()).isEqualTo(tenantId);
        assertThat(actor.principal()).isEqualTo("alice");
        assertThat(actor.authorities()).containsExactly("ROLE_USER");
    }

    @Test
    void fallbackToUuidPrincipalStillWorks() {
        UUID actorId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                actorId.toString(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        CurrentActor actor = bridge.getCurrentActor();
        assertThat(actor.id()).isEqualTo(actorId);
    }

    @Test
    void nonUuidPrincipalFallsBackToAnonymous() {
        var auth = new UsernamePasswordAuthenticationToken(
                "opaque", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(bridge.getCurrentActor().type()).isEqualTo(ActorType.ANONYMOUS);
    }
}