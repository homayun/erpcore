package com.erp.core.security.internal.authentication.application;

import java.util.UUID;

public record AuthenticationCommand(
        UUID tenantId,
        String username,
        String password
) {
    public AuthenticationCommand {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password is required");
        }
    }
}