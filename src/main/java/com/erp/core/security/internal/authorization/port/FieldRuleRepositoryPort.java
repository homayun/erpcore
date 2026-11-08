package com.erp.core.security.internal.authorization.port;

import com.erp.core.security.internal.authorization.domain.FieldRule;

import java.util.List;

public interface FieldRuleRepositoryPort {

    List<FieldRule> findByResourceType(String resourceType);

    List<FieldRule> findAll();

    FieldRule upsert(FieldRule rule);

    boolean delete(String resourceType, String fieldName);

    boolean exists(String resourceType, String fieldName);
}