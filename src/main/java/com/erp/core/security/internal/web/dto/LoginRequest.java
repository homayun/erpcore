package com.erp.core.security.internal.web.dto;

import java.util.UUID;

public record LoginRequest(UUID tenantId, String username, String password) {
}