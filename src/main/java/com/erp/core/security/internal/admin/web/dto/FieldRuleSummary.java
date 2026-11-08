package com.erp.core.security.internal.admin.web.dto;

public record FieldRuleSummary(
        String resourceType,
        String fieldName,
        String requiredPermission,
        String maskingWhenDenied
) {}