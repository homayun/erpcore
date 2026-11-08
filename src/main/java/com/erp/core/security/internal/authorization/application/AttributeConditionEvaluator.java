package com.erp.core.security.internal.authorization.application;

import com.erp.core.security.internal.authorization.domain.AttributeCondition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluates an ABAC condition against a request's attribute map.
 *
 * Design constraints:
 * - Numeric comparisons use {@link BigDecimal} to avoid floating-point loss.
 * - Missing attributes evaluate to {@code false} (deny-by-default).
 * - Malformed attribute values (non-numeric) also evaluate to {@code false}.
 *   This is critical: an LTE/GTE comparison must NEVER silently pass when
 *   the attribute cannot be compared numerically. Attacker-controlled
 *   values like "abc" must NOT bypass a numeric upper-bound check.
 * - This evaluator never throws during authorization â€” it fails closed.
 */
public class AttributeConditionEvaluator {

    public boolean evaluate(AttributeCondition condition, Map<String, Object> attributes) {
        if (condition == null) return true;
        if (attributes == null) return false;

        Object actual = attributes.get(condition.attributeName());
        if (actual == null) return false;

        return switch (condition.operator()) {
            case EQUALS -> Objects.equals(String.valueOf(actual), condition.value());
            case IN -> List.of(condition.value().split(","))
                    .stream().map(String::trim)
                    .anyMatch(v -> v.equals(String.valueOf(actual)));
            case LTE -> {
                Integer cmp = compareNumeric(actual, condition.value());
                yield cmp != null && cmp <= 0;
            }
            case GTE -> {
                Integer cmp = compareNumeric(actual, condition.value());
                yield cmp != null && cmp >= 0;
            }
            case CONTAINS_ANY -> {
                String haystack = String.valueOf(actual);
                yield List.of(condition.value().split(","))
                        .stream().map(String::trim)
                        .anyMatch(haystack::contains);
            }
        };
    }

    /**
     * Returns the numeric comparison result, or {@code null} if either side
     * is not a valid number. Callers MUST treat null as "condition not
     * satisfied" â€” never as "equal".
     */
    private Integer compareNumeric(Object actual, String expected) {
        try {
            return new BigDecimal(String.valueOf(actual))
                    .compareTo(new BigDecimal(expected));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}