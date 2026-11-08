package com.erp.core.security.internal.context.infrastructure.springsecurity;

import com.erp.core.security.api.actor.ActorType;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpUserDetails;
import com.erp.core.security.internal.data.application.TenantContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpringSecurityActorBridge implements CurrentActorProvider {

    private static final CurrentActor ANONYMOUS = new CurrentActor(
            null, ActorType.ANONYMOUS, null, null, Set.of());

    @Override
    public CurrentActor getCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return ANONYMOUS;
        }

        // JWT callers (/api/**): identity comes from validated claims.
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            UUID actorId = extractUuid(jwt.getClaimAsString("actor_id"));
            UUID tenantId = extractUuid(jwt.getClaimAsString("tenant_id"));
            if (actorId == null || tenantId == null) {
                return ANONYMOUS;
            }
            Set<String> authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toUnmodifiableSet());
            return new CurrentActor(actorId, ActorType.USER, tenantId, jwt.getSubject(), authorities);
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof ErpUserDetails user) {
            Set<String> authorities = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toUnmodifiableSet());
            return new CurrentActor(
                    user.actorId(), ActorType.USER, user.tenantId(),
                    user.getUsername(), authorities);
        }

        // Fallback for tests using plain UsernamePasswordAuthenticationToken
        UUID actorId = extractUuid(principal);
        if (actorId == null) {
            return ANONYMOUS;
        }
        UUID tenantId = TenantContextHolder.current().orElse(null);
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return new CurrentActor(actorId, ActorType.USER, tenantId, auth.getName(), authorities);
    }

    private UUID extractUuid(Object principal) {
        if (principal instanceof UUID uuid) return uuid;
        if (principal instanceof String text) {
            try { return UUID.fromString(text); }
            catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }
}