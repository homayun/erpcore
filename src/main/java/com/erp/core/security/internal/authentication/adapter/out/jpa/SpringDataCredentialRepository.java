package com.erp.core.security.internal.authentication.adapter.out.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataCredentialRepository extends JpaRepository<CredentialEntity, UUID> {
    Optional<CredentialEntity> findByTenantIdAndUsername(UUID tenantId, String username);
    Page<CredentialEntity> findByTenantId(UUID tenantId, Pageable pageable);
    long countByTenantId(UUID tenantId);
    boolean existsByTenantIdAndUsername(UUID tenantId, String username);
}