package com.erp.core.security.api.tenant;

import java.util.UUID;

public record TenantScope(UUID tenantId) {
    public TenantScope {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant id is required");
        }
    }
}