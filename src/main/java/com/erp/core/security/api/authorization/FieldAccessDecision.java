package com.erp.core.security.api.authorization;

import java.util.Map;
import java.util.Set;

public record FieldAccessDecision(
        Set<String> grantedFields,
        Map<String, MaskingStrategy> maskings
) {
    public FieldAccessDecision {
        grantedFields = grantedFields == null ? Set.of() : Set.copyOf(grantedFields);
        maskings = maskings == null ? Map.of() : Map.copyOf(maskings);
    }

    public static FieldAccessDecision denyAll() {
        return new FieldAccessDecision(Set.of(), Map.of());
    }

    public enum MaskingStrategy { FULL, PARTIAL, HASH, REDACT }
}