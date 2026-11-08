package com.erp.core.security.internal.data.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-bound tenant context.
 *
 * Phase 01: ThreadLocal storage. Phase 07 (ScopedValue migration) replaces
 * this with a ScopedValue-based implementation. Callers MUST use the
 * accessor methods only â€” never the underlying storage.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void bind(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}