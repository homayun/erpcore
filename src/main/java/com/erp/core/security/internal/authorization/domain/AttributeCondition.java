package com.erp.core.security.internal.authorization.domain;

/**
 * Structured ABAC condition. Example: {@code amount <= 10000} expressed as
 * {@code AttributeCondition("amount", LTE, "10000")}.
 *
 * Evaluation is local and in-process â€” no external policy engine. This is
 * intentional: ERP-grade rules rarely need XACML, and the added complexity
 * would outweigh the benefit for the module's target scope.
 */
public record AttributeCondition(
        String attributeName,
        Operator operator,
        String value
) {
    public AttributeCondition {
        if (attributeName == null || attributeName.isBlank()) {
            throw new IllegalArgumentException("Attribute name is required");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator is required");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value is required");
        }
    }

    public enum Operator {
        EQUALS,
        IN,           // comma-separated value list
        LTE,
        GTE,
        CONTAINS_ANY  // comma-separated substring list
    }
}