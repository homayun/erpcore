package com.erp.core.admin.web.dto;

import java.util.UUID;

public record CreateUserRequest(
        UUID tenantId,
        String username,
        String password
) {}