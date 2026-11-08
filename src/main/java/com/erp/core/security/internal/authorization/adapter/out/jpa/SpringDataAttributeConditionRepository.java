package com.erp.core.security.internal.authorization.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataAttributeConditionRepository
        extends JpaRepository<AttributeConditionEntity, UUID> {
    List<AttributeConditionEntity> findByPermissionKey(String permissionKey);
}