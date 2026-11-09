package com.erp.core.admin.web.dto;

public record CreateFieldRuleRequest(
        String resourceType,
        String fieldName,
        String requiredPermission,
        String maskingWhenDenied
) {}