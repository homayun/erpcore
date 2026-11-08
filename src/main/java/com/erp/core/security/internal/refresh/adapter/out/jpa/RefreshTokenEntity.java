package com.erp.core.security.internal.refresh.adapter.out.jpa;

import com.erp.core.security.internal.refresh.domain.RefreshTokenRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sec_refresh_token",
        indexes = {
                @Index(name = "idx_rt_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_rt_family", columnList = "family_id")
        })
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    protected RefreshTokenEntity() {}

    public static RefreshTokenEntity fromDomain(RefreshTokenRecord r) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.id = r.id();
        e.actorId = r.actorId();
        e.tenantId = r.tenantId();
        e.tokenHash = r.tokenHash();
        e.familyId = r.familyId();
        e.createdAt = r.createdAt();
        e.expiresAt = r.expiresAt();
        e.revokedAt = r.revokedAt();
        e.replacedBy = r.replacedBy();
        return e;
    }

    public RefreshTokenRecord toDomain() {
        return new RefreshTokenRecord(
                id, actorId, tenantId, tokenHash, familyId,
                createdAt, expiresAt, revokedAt, replacedBy);
    }

    public UUID getId() { return id; }
    public UUID getFamilyId() { return familyId; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant v) { this.revokedAt = v; }
    public void setReplacedBy(UUID v) { this.replacedBy = v; }
}