package com.erp.core.admin.internal.setting.domain;
import java.time.Instant;
import java.util.UUID;
public record CodeSetting(UUID id, UUID tenantId, String category, String code, String name, boolean isActive, Instant createdAt) {}