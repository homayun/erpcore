package com.erp.core.security.internal.authorization;

import com.erp.core.security.internal.authorization.application.AttributeConditionEvaluator;
import com.erp.core.security.internal.authorization.domain.AttributeCondition;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeConditionEvaluatorTest {

    private final AttributeConditionEvaluator ev = new AttributeConditionEvaluator();

    @Test
    void equalsMatchesExactValue() {
        var c = new AttributeCondition("department", AttributeCondition.Operator.EQUALS, "FIN");
        assertThat(ev.evaluate(c, Map.of("department", "FIN"))).isTrue();
        assertThat(ev.evaluate(c, Map.of("department", "HR"))).isFalse();
    }

    @Test
    void inMatchesAnyToken() {
        var c = new AttributeCondition("region", AttributeCondition.Operator.IN, "EU,US");
        assertThat(ev.evaluate(c, Map.of("region", "US"))).isTrue();
        assertThat(ev.evaluate(c, Map.of("region", "APAC"))).isFalse();
    }

    @Test
    void lteUsesBigDecimal() {
        var c = new AttributeCondition("amount", AttributeCondition.Operator.LTE, "10000.50");
        assertThat(ev.evaluate(c, Map.of("amount", 10000.49))).isTrue();
        assertThat(ev.evaluate(c, Map.of("amount", 10001))).isFalse();
    }

    @Test
    void missingAttributeFailsClosed() {
        var c = new AttributeCondition("amount", AttributeCondition.Operator.LTE, "10000");
        assertThat(ev.evaluate(c, Map.of())).isFalse();
    }

    @Test
    void nonNumericValueFailsClosed() {
        var c = new AttributeCondition("amount", AttributeCondition.Operator.LTE, "10000");
        assertThat(ev.evaluate(c, Map.of("amount", "not-a-number"))).isFalse();
    }

    @Test
    void containsAnyIsSubstring() {
        var c = new AttributeCondition("tags", AttributeCondition.Operator.CONTAINS_ANY, "vip,priority");
        assertThat(ev.evaluate(c, Map.of("tags", "customer-vip-gold"))).isTrue();
        assertThat(ev.evaluate(c, Map.of("tags", "standard"))).isFalse();
    }
}