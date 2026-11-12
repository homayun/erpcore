package com.erp.core.admin.tenant.adapter.out.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sec_tenant")
public class TenantEntity {

    public enum Status { ACTIVE, SUSPENDED, ARCHIVED }

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    protected TenantEntity() {}

    public static TenantEntity create(UUID id, String code, String name,
                                      String subscriptionPlan, UUID createdBy) {
        TenantEntity e = new TenantEntity();
        e.id = id;
        e.code = code.toUpperCase();
        e.name = name;
        e.status = Status.ACTIVE;
        e.subscriptionPlan = subscriptionPlan;
        e.createdAt = Instant.now();
        e.createdBy = createdBy;
        return e;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Status getStatus() { return status; }
    public String getSubscriptionPlan() { return subscriptionPlan; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }

    public void setStatus(Status status) { this.status = status; }
    public void updateMetadata(String name, String subscriptionPlan) {
        this.name = name;
        this.subscriptionPlan = subscriptionPlan;
    }
}