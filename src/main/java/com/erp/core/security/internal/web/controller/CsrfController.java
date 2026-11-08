package com.erp.core.security.internal.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Returns the current session's CSRF token for JSON/SPA clients.
 * Session cookie is created on first call; subsequent POST /login must
 * include the token via X-CSRF-TOKEN header.
 */
@RestController
public class CsrfController {

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            return Map.of();
        }
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}