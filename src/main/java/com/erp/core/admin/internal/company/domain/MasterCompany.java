package com.erp.core.admin.internal.company.domain;
import java.time.Instant;
import java.util.UUID;
public record MasterCompany(UUID id, UUID tenantId, String name, String code, boolean isActive, Instant createdAt) {}