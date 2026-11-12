package com.erp.core.admin.tenant.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataTenantRepository extends JpaRepository<TenantEntity, UUID> {

    Optional<TenantEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<TenantEntity> findAllByOrderByCreatedAtDesc();
}