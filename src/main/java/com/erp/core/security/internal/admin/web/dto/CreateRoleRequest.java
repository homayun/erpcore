package com.erp.core.security.internal.admin.web.dto;

import java.util.UUID;

public record CreateRoleRequest(UUID tenantId, String name, Integer hierarchyLevel) {}