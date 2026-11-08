package com.erp.core.security.internal.authorization.domain;

import java.util.UUID;

public record Role(UUID id, UUID tenantId, String name, int hierarchyLevel) {
    public Role {
        if (id == null) throw new IllegalArgumentException("Role id is required");
        if (tenantId == null) throw new IllegalArgumentException("Tenant id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Role name is required");
    }
}