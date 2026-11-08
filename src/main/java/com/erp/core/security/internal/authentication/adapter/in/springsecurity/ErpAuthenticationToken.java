package com.erp.core.security.internal.authentication.adapter.in.springsecurity;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

public final class ErpAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID tenantId;
    private final String username;
    private final Object principal;
    private String credentials;

    public ErpAuthenticationToken(UUID tenantId, String username, String password) {
        super((Collection<? extends GrantedAuthority>) null);
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username is required");
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("password is required");
        this.tenantId = tenantId;
        this.username = username;
        this.principal = username;
        this.credentials = password;
        setAuthenticated(false);
    }

    public ErpAuthenticationToken(ErpUserDetails principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        if (principal == null) throw new IllegalArgumentException("principal is required");
        this.tenantId = principal.tenantId();
        this.username = principal.getUsername();
        this.principal = principal;
        this.credentials = null;
        setAuthenticated(true);
    }

    public UUID tenantId() { return tenantId; }
    public String username() { return username; }

    @Override public Object getCredentials() { return credentials; }
    @Override public Object getPrincipal() { return principal; }

    @Override public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}