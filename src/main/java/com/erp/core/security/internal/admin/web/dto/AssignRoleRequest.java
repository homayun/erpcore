package com.erp.core.security.internal.admin.web.dto;

import java.util.UUID;

public record AssignRoleRequest(UUID actorId, UUID tenantId) {}