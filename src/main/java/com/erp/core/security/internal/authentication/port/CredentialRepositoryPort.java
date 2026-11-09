package com.erp.core.security.internal.authentication.port;

import com.erp.core.security.internal.authentication.domain.Credential;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredentialRepositoryPort {

    Optional<Credential> findByUsernameAndTenant(UUID tenantId, String username);

    void save(Credential credential);

    void updatePasswordHash(UUID actorId, String newHash);

    // ---- Admin-facing operations ----
    List<Credential> findByTenant(UUID tenantId, int page, int size);

    long countByTenant(UUID tenantId);

    boolean existsByUsername(UUID tenantId, String username);

    void setEnabled(UUID actorId, UUID tenantId, boolean enabled);

    void updateUser(UUID actorId, UUID tenantId, String newUsername, String newPasswordHash);

    void deleteUser(UUID actorId, UUID tenantId);
}