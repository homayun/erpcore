package com.erp.core.security.internal.authorization.port;

import com.erp.core.security.internal.authorization.domain.AttributeCondition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;

import java.util.List;

public interface AttributeConditionRepositoryPort {
    List<AttributeCondition> findForPermission(PermissionKey key);
}