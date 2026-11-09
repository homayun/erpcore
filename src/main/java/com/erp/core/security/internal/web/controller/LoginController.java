package com.erp.core.security.internal.web.controller;

import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpAuthenticationToken;
import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpUserDetails;
import com.erp.core.security.internal.authentication.port.AccountLockoutPort;
import com.erp.core.security.internal.authentication.port.CredentialStatusPort;
import com.erp.core.security.internal.jwt.application.JwtTokenIssuer;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import com.erp.core.security.internal.refresh.application.RefreshTokenService;
import com.erp.core.security.internal.web.dto.LoginRequest;
import com.erp.core.security.internal.web.dto.TokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class LoginController {

    /** Fallback tenant ID used when the request does not specify one (single-tenant UI mode). */
    private static final UUID FALLBACK_TENANT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final AuthenticationManager authenticationManager;
    private final JwtTokenIssuer jwtIssuer;
    private final RefreshTokenService refreshService;
    private final AccountLockoutPort lockoutPort;
    private final SecurityProperties props;
    private final CredentialStatusPort statusPort;
    private final UUID defaultTenantId;

    /** 6-arg constructor used by SecurityModuleAutoConfiguration (single-tenant default). */
    public LoginController(AuthenticationManager authenticationManager,
                           JwtTokenIssuer jwtIssuer,
                           RefreshTokenService refreshService,
                           AccountLockoutPort lockoutPort,
                           SecurityProperties props,
                           CredentialStatusPort statusPort) {
        this(authenticationManager, jwtIssuer, refreshService, lockoutPort, props, statusPort,
             FALLBACK_TENANT_ID);
    }

    /** 7-arg constructor for tests / multi-tenant wiring. */
    public LoginController(AuthenticationManager authenticationManager,
                           JwtTokenIssuer jwtIssuer,
                           RefreshTokenService refreshService,
                           AccountLockoutPort lockoutPort,
                           SecurityProperties props,
                           CredentialStatusPort statusPort,
                           UUID defaultTenantId) {
        this.authenticationManager = authenticationManager;
        this.jwtIssuer = jwtIssuer;
        this.refreshService = refreshService;
        this.lockoutPort = lockoutPort;
        this.props = props;
        this.statusPort = statusPort;
        this.defaultTenantId = defaultTenantId;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response) {
        try {
            UUID tenantId = request.tenantId() != null ? request.tenantId() : defaultTenantId;

            Authentication auth = authenticationManager.authenticate(
                    new ErpAuthenticationToken(tenantId, request.username(), request.password()));

            ErpUserDetails user = (ErpUserDetails) auth.getPrincipal();

            boolean enabled = statusPort.find(user.actorId(), user.tenantId())
                    .map(CredentialStatusPort.CredentialStatus::enabled)
                    .orElse(false);
            if (!enabled) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            lockoutPort.recordSuccess(user.actorId());

            // Persist SecurityContext into HTTP session so browser requests are authenticated.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            httpRequest.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            Set<String> authorities = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toUnmodifiableSet());

            String accessToken = jwtIssuer.issue(
                    user.actorId(), user.tenantId(), user.getUsername(), authorities);

            RefreshTokenService.IssuedRefreshToken refresh =
                    refreshService.issue(user.actorId(), user.tenantId());

            attachRefreshCookie(response, refresh.rawToken());

            return ResponseEntity.ok(new TokenResponse(
                    accessToken, "Bearer",
                    props.jwt().accessTtl().toSeconds(),
                    user.actorId(), user.tenantId(), user.getUsername()));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private void attachRefreshCookie(HttpServletResponse response, String rawToken) {
        Cookie cookie = new Cookie(props.refreshToken().cookieName(), rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge((int) props.refreshToken().ttl().toSeconds());
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}