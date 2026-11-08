package com.erp.core.security.internal.web.controller;

import com.erp.core.security.internal.authentication.port.CredentialStatusPort;
import com.erp.core.security.internal.jwt.application.JwtTokenIssuer;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import com.erp.core.security.internal.refresh.application.RefreshTokenService;
import com.erp.core.security.internal.web.dto.TokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.Set;

/**
 * Rotates refresh token. Detects reuse and revokes the entire family.
 * After rotation the account is re-checked: a deleted or disabled account
 * has its whole token family revoked and gets a generic 401.
 */
@RestController
public class RefreshController {

    private final RefreshTokenService refreshService;
    private final JwtTokenIssuer jwtIssuer;
    private final SecurityProperties props;
    private final CredentialStatusPort statusPort;

    public RefreshController(RefreshTokenService refreshService,
                             JwtTokenIssuer jwtIssuer,
                             SecurityProperties props,
                             CredentialStatusPort statusPort) {
        this.refreshService = refreshService;
        this.jwtIssuer = jwtIssuer;
        this.props = props;
        this.statusPort = statusPort;
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String raw = readCookie(request, props.refreshToken().cookieName());
        if (raw == null || raw.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            RefreshTokenService.RotatedRefreshToken rotated = refreshService.rotate(raw);

            Optional<CredentialStatusPort.CredentialStatus> status =
                    statusPort.find(rotated.actorId(), rotated.tenantId());
            if (status.isEmpty() || !status.get().enabled()) {
                refreshService.revoke(rotated.rawToken());   // revokes the whole family
                clearCookie(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String username = status.get().username();
            String accessToken = jwtIssuer.issue(
                    rotated.actorId(), rotated.tenantId(), username, Set.of("ROLE_USER"));
            attachRefreshCookie(response, rotated.rawToken());
            return ResponseEntity.ok(new TokenResponse(
                    accessToken, "Bearer", props.jwt().accessTtl().toSeconds(),
                    rotated.actorId(), rotated.tenantId(), username));
        } catch (RefreshTokenService.RefreshTokenReuseDetectedException ex) {
            clearCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RefreshTokenService.InvalidRefreshTokenException ex) {
            clearCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
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

    private void clearCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(props.refreshToken().cookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}