package com.erp.core.security.internal.authorization.application;

import com.erp.core.security.api.permission.PermissionDeclaration;
import com.erp.core.security.api.permission.PermissionDeclarationPort;
import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implements the public {@link PermissionDeclarationPort} so business modules
 * can register their permissions at startup.
 *
 * Behaviour:
 * - Validates every key's format (UPPER_SNAKE_CASE, 3-100 chars).
 * - Idempotent: existing rows are updated, new rows inserted.
 * - Fail-fast: invalid key aborts the whole batch (better to fail at boot
 *   than to ship a half-registered catalog).
 */
public class PermissionRegistryService implements PermissionDeclarationPort {

    private static final Logger log = LoggerFactory.getLogger(PermissionRegistryService.class);

    private final PermissionRepositoryPort repository;

    public PermissionRegistryService(PermissionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public void registerPermissions(List<PermissionDeclaration> declarations) {
        if (declarations == null || declarations.isEmpty()) return;

        List<PermissionDefinition> definitions = declarations.stream()
                .map(d -> new PermissionDefinition(
                        PermissionKey.of(d.key()), d.group(), d.description(), d.sensitive()))
                .toList();

        repository.upsertAll(definitions);
        log.info("Registered {} permissions into the ErpCore security catalog", definitions.size());
    }
}