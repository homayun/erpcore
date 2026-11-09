package com.erp.core.admin.web.dto;

import java.util.Set;
import java.util.UUID;

public record RoleSummary(UUID id, UUID tenantId, String name, int hierarchyLevel,
                          Set<String> permissionKeys, boolean isActive) {}