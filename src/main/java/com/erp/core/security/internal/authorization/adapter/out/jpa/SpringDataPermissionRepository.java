package com.erp.core.security.internal.authorization.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPermissionRepository extends JpaRepository<PermissionEntity, String> {
}