package com.erp.core.security.internal.authorization.adapter.out.jpa;

import com.erp.core.security.internal.authorization.domain.FieldRule;
import com.erp.core.security.internal.authorization.port.FieldRuleRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class JpaFieldRuleRepositoryAdapter implements FieldRuleRepositoryPort {

    private final SpringDataFieldRuleRepository repo;

    public JpaFieldRuleRepositoryAdapter(SpringDataFieldRuleRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldRule> findByResourceType(String resourceType) {
        return repo.findByResourceType(resourceType).stream()
                .map(FieldRuleEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldRule> findAll() {
        return repo.findAll().stream().map(FieldRuleEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public FieldRule upsert(FieldRule rule) {
        var existing = repo.findByResourceTypeAndFieldName(rule.resourceType(), rule.fieldName());
        if (existing.isPresent()) {
            FieldRuleEntity entity = existing.get();
            entity.applyUpdate(rule);
            repo.save(entity);
            return entity.toDomain();
        }
        FieldRuleEntity entity = FieldRuleEntity.fromDomain(rule);
        repo.save(entity);
        return entity.toDomain();
    }

    @Override
    @Transactional
    public boolean delete(String resourceType, String fieldName) {
        return repo.deleteByResourceTypeAndFieldName(resourceType, fieldName) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String resourceType, String fieldName) {
        return repo.existsByResourceTypeAndFieldName(resourceType, fieldName);
    }
}