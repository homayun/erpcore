package com.erp.core.security.internal.admin.web.dto;

public record CreateFieldRuleRequest(
        String resourceType,
        String fieldName,
        String requiredPermission,
        String maskingWhenDenied
) {}