package com.erp.core.security.internal.authorization.domain;

/**
 * Declarative per-field authorization rule.
 *
 * Semantics:
 * - No rule for a field  -> visible once resource-level check passes.
 * - Rule + permission    -> full access.
 * - Rule + no permission -> value masked per maskingWhenDenied.
 */
public record FieldRule(
        String resourceType,
        String fieldName,
        PermissionKey requiredPermission,
        MaskingWhenDenied maskingWhenDenied
) {
    public FieldRule {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType is required");
        }
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("fieldName is required");
        }
        if (requiredPermission == null) {
            throw new IllegalArgumentException("requiredPermission is required");
        }
        if (maskingWhenDenied == null) {
            throw new IllegalArgumentException("maskingWhenDenied is required");
        }
    }

    public enum MaskingWhenDenied {
        HIDDEN, REDACT, PARTIAL, HASH
    }
}