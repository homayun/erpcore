package com.erp.core.security.internal.authentication.adapter.out.jpa;

import com.erp.core.security.internal.authentication.domain.Credential;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "sec_credentials",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_credentials_tenant_username",
                columnNames = {"tenant_id", "username"}))
public class CredentialEntity {

    @Id
    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected CredentialEntity() {}

    public static CredentialEntity fromDomain(Credential c) {
        CredentialEntity e = new CredentialEntity();
        e.actorId = c.actorId();
        e.tenantId = c.tenantId();
        e.username = c.username();
        e.passwordHash = c.passwordHash();
        e.enabled = c.enabled();
        return e;
    }

    public Credential toDomain() {
        return new Credential(actorId, tenantId, username, passwordHash, enabled);
    }

    public UUID getActorId() { return actorId; }
    public UUID getTenantId() { return tenantId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }

    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}