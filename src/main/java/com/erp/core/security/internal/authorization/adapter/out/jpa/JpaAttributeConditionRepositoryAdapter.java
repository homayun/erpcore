package com.erp.core.security.internal.authorization.adapter.out.jpa;

import com.erp.core.security.internal.authorization.domain.AttributeCondition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.AttributeConditionRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class JpaAttributeConditionRepositoryAdapter implements AttributeConditionRepositoryPort {

    private final SpringDataAttributeConditionRepository repo;

    public JpaAttributeConditionRepositoryAdapter(SpringDataAttributeConditionRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeCondition> findForPermission(PermissionKey key) {
        return repo.findByPermissionKey(key.value()).stream()
                .map(AttributeConditionEntity::toDomain)
                .toList();
    }
}