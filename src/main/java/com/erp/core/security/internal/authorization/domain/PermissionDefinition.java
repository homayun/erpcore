package com.erp.core.security.internal.authorization.domain;

/**
 * Registered permission (declared by a business module via
 * {@code PermissionDeclarationPort}).
 */
public record PermissionDefinition(
        PermissionKey key,
        String group,
        String description,
        boolean sensitive
) {
    public PermissionDefinition {
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("Permission group is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Permission description is required");
        }
    }
}