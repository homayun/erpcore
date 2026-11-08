package com.erp.core.security.api.permission;

public record PermissionDeclaration(
        String key,
        String group,
        String description,
        boolean sensitive
) {
    public PermissionDeclaration {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Permission key is required");
        }
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("Permission group is required");
        }
    }
}