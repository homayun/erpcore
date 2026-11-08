package com.erp.core.security.api.authorization;

import java.util.UUID;

public record ResourceDescriptor(
        String type,
        String externalReference,
        UUID tenantId
) {
    public ResourceDescriptor {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Resource type is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant id is required on resource");
        }
    }
}