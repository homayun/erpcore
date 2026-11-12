package com.erp.core.security.internal.context.infrastructure.springsecurity;

import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpUserDetails;
import com.erp.core.security.internal.data.application.TenantContextHolder;
import com.erp.core.security.internal.tenant.port.TenantMembershipPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Binds the caller's ACTIVE tenant to TenantContextHolder.
 *
 * Two paths:
 *  - JWT callers (stateless): use the signed tenant_id claim. A session
 *    override is NEVER honored — the JWT was issued with the caller's
 *    default tenant and must not be silently redirected.
 *  - Session callers (browser): prefer the per-session override
 *    (erp.activeTenantId), but ONLY if the actor is a verified member of
 *    that tenant. A tampered override is stripped, and the caller falls
 *    back to their primary tenant (ErpUserDetails.tenantId()).
 *
 * The context is cleared in finally{} so pooled threads never leak the
 * previous request's tenant.
 */
public class TenantContextRestorationFilter extends OncePerRequestFilter {

    public static final String ACTIVE_TENANT_SESSION_ATTR = "erp.activeTenantId";

    private static final Logger log = LoggerFactory.getLogger(TenantContextRestorationFilter.class);

    private final TenantMembershipPort membership;

    public TenantContextRestorationFilter() { this(null); }

    public TenantContextRestorationFilter(TenantMembershipPort membership) {
        this.membership = membership;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                // JWT path: signed claim — never overridden by session.
                UUID tenantId = parseUuid(jwtAuth.getToken().getClaimAsString("tenant_id"));
                if (tenantId != null) TenantContextHolder.bind(tenantId);
            } else if (auth != null && auth.getPrincipal() instanceof ErpUserDetails user) {
                UUID tenantId = resolveSessionTenant(user, request);
                if (tenantId != null) TenantContextHolder.bind(tenantId);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private UUID resolveSessionTenant(ErpUserDetails user, HttpServletRequest request) {
        UUID primary = user.tenantId();
        HttpSession session = request.getSession(false);
        if (session == null) return primary;

        Object override = session.getAttribute(ACTIVE_TENANT_SESSION_ATTR);
        if (!(override instanceof UUID candidate)) return primary;
        if (candidate.equals(primary)) return primary;

        if (membership != null && membership.isMemberOf(user.actorId(), candidate)) {
            return candidate;
        }
        log.warn("Rejected tenant override {} for actor={} — not a member",
                candidate, user.actorId());
        session.removeAttribute(ACTIVE_TENANT_SESSION_ATTR);
        return primary;
    }

    private static UUID parseUuid(String value) {
        if (value == null) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException e) { return null; }
    }
}