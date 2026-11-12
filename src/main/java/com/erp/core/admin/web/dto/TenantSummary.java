package com.erp.core.admin.web.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantSummary(
        UUID id,
        String code,
        String name,
        String status,
        String subscriptionPlan,
        Instant createdAt
) {}