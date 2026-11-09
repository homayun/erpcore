package com.erp.core.admin.internal.company.domain;
import java.time.Instant;
import java.util.UUID;
public record Company(UUID id, UUID tenantId, String name, String registrationNumber, String taxId, String email, String phone, String address, boolean isActive, Instant createdAt) {}