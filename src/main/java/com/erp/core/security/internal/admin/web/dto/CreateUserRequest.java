package com.erp.core.security.internal.admin.web.dto;

import java.util.UUID;

public record CreateUserRequest(
        UUID tenantId,
        String username,
        String password
) {}