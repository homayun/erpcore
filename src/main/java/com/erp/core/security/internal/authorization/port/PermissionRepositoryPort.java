package com.erp.core.security.internal.authorization.port;

import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;

import java.util.Collection;
import java.util.List;

public interface PermissionRepositoryPort {
    void upsertAll(Collection<PermissionDefinition> definitions);
    boolean exists(PermissionKey key);
    List<PermissionDefinition> findAll();
}