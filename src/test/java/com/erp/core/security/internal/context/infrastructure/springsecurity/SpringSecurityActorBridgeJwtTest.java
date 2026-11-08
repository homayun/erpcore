package com.erp.core.security.internal.context.infrastructure.springsecurity;

import com.erp.core.security.api.actor.ActorType;
import com.erp.core.security.api.actor.CurrentActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SpringSecurityActorBridgeJwtTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Jwt jwt(String actorId, String tenantId) {
        return Jwt.withTokenValue("t")
                .header("alg", "HS256")
                .subject("alice")
                .claim("actor_id", actorId)
                .claim("tenant_id", tenantId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    @Test
    void jwtCallerIsMappedToActorWithTenant() {
        UUID actor = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt(actor.toString(), tenant.toString()),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        CurrentActor current = new SpringSecurityActorBridge().getCurrentActor();

        assertThat(current.type()).isEqualTo(ActorType.USER);
        assertThat(current.id()).isEqualTo(actor);
        assertThat(current.tenantId()).isEqualTo(tenant);
        assertThat(current.authorities()).contains("ROLE_USER");
    }

    @Test
    void jwtWithoutValidIdentityClaimsIsAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt("not-a-uuid", UUID.randomUUID().toString()),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(new SpringSecurityActorBridge().getCurrentActor().type()).isEqualTo(ActorType.ANONYMOUS);
    }
}