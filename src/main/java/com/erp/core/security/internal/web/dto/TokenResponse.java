package com.erp.core.security.internal.web.dto;

import java.util.UUID;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID actorId,
        UUID tenantId,
        String username
) {
    public TokenResponse {
        if (tokenType == null) tokenType = "Bearer";
    }
}