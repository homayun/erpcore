package com.erp.core.admin.internal.company.domain;
import java.time.Instant;
import java.util.UUID;
public record Branch(UUID id, UUID tenantId, UUID companyId, String name, String branchCode, String address, String phone, boolean isActive, Instant createdAt) {}