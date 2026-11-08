package com.erp.core.security.internal.web.dto;

import java.util.UUID;

public record LoginResponse(UUID actorId, UUID tenantId, String username) {
}