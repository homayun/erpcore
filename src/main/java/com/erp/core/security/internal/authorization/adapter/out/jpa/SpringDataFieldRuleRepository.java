package com.erp.core.security.internal.authorization.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataFieldRuleRepository extends JpaRepository<FieldRuleEntity, UUID> {

    List<FieldRuleEntity> findByResourceType(String resourceType);

    Optional<FieldRuleEntity> findByResourceTypeAndFieldName(String resourceType, String fieldName);

    boolean existsByResourceTypeAndFieldName(String resourceType, String fieldName);

    long deleteByResourceTypeAndFieldName(String resourceType, String fieldName);
}