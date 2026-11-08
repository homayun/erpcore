package com.erp.core.security.internal.authorization.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Canonical permission identifier (e.g. {@code FINANCE_INVOICE_APPROVE}).
 *
 * Format: UPPER_SNAKE_CASE, 3-100 chars. Enforced at construction so that
 * misconfigured declarations fail fast at startup, not at authorization time.
 */
public record PermissionKey(String value) {

    private static final Pattern VALID = Pattern.compile("^[A-Z][A-Z0-9_]{2,99}$");

    public PermissionKey {
        Objects.requireNonNull(value, "Permission key is required");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid permission key '" + value + "' â€” must match " + VALID.pattern());
        }
    }

    public static PermissionKey of(String value) {
        return new PermissionKey(value);
    }

    @Override public String toString() { return value; }
}