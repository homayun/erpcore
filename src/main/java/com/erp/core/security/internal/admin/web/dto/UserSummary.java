package com.erp.core.security.internal.admin.web.dto;

import java.util.UUID;

public record UserSummary(UUID actorId, UUID tenantId, String username, boolean enabled) {}