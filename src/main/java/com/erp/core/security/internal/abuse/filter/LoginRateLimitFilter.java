package com.erp.core.security.internal.abuse.filter;

import com.erp.core.security.internal.abuse.port.RateLimitPort;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-client-IP rate limit for the login endpoint.
 *
 * SECURITY: the key is request.getRemoteAddr() ONLY. The X-Forwarded-For header
 * is attacker-controlled; trusting it lets a client pick a fresh bucket for
 * every request. Behind a reverse proxy, set
 *   server.forward-headers-strategy=native
 * (and server.tomcat.remoteip.internal-proxies) so the container resolves the
 * real client address from TRUSTED proxies only.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitPort rateLimitPort;
    private final SecurityProperties props;

    public LoginRateLimitFilter(RateLimitPort rateLimitPort, SecurityProperties props) {
        this.rateLimitPort = rateLimitPort;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !props.login().processingUrl().equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String key = "login:" + request.getRemoteAddr();
        if (!rateLimitPort.tryConsume(key)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":\"SEC-4290\",\"message\":\"Too many login attempts\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}