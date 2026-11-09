package com.erp.core.admin.web.dto;

import java.util.UUID;

public record AssignRoleRequest(UUID actorId, UUID tenantId) {}