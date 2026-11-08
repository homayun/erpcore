package com.erp.core.security.internal.admin.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAdminRoleRepository extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByTenantIdAndName(UUID tenantId, String name);
    List<RoleEntity> findByTenantId(UUID tenantId);
}