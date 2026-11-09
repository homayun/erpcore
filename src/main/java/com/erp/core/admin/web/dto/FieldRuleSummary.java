package com.erp.core.admin.web.dto;

public record FieldRuleSummary(
        String resourceType,
        String fieldName,
        String requiredPermission,
        String maskingWhenDenied
) {}