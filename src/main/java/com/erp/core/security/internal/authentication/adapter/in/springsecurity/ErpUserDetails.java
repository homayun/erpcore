package com.erp.core.security.internal.authentication.adapter.in.springsecurity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Authenticated principal for the ErpCore Security module.
 *
 * SECURITY:
 * - `password` is intentionally empty â€” password hashes MUST NEVER be stored
 *   in SecurityContext or session.
 * - `actorId` and `tenantId` are UUIDs (not user-facing).
 */
public final class ErpUserDetails implements UserDetails {

    private final UUID actorId;
    private final UUID tenantId;
    private final String username;
    private final Set<GrantedAuthority> authorities;

    public ErpUserDetails(UUID actorId, UUID tenantId, String username, Collection<? extends GrantedAuthority> authorities) {
        if (actorId == null) throw new IllegalArgumentException("actorId is required");
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username is required");
        this.actorId = actorId;
        this.tenantId = tenantId;
        this.username = username;
        this.authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
    }

    public UUID actorId() { return actorId; }
    public UUID tenantId() { return tenantId; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return ""; }   // never stored
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}