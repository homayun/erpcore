package com.erp.core.security.internal.context.infrastructure.springsecurity;

import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpUserDetails;
import com.erp.core.security.internal.data.application.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Binds the authenticated caller's tenant to TenantContextHolder for the
 * duration of the request, then clears it (no leakage across pooled threads).
 *
 * Supports both session principals (ErpUserDetails) and JWT callers
 * (tenant_id claim, already validated as a UUID by the JwtDecoder).
 */
public class TenantContextRestorationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                UUID tenantId = parseUuid(jwtAuth.getToken().getClaimAsString("tenant_id"));
                if (tenantId != null) {
                    TenantContextHolder.bind(tenantId);
                }
            } else if (auth != null && auth.getPrincipal() instanceof ErpUserDetails user) {
                TenantContextHolder.bind(user.tenantId());
            }
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException e) { return null; }
    }
}