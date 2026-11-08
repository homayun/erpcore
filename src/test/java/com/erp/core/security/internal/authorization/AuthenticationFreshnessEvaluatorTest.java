package com.erp.core.security.internal.authorization;

import com.erp.core.security.internal.authorization.application.AuthenticationFreshnessEvaluator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationFreshnessEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private final Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AuthenticationFreshnessEvaluator evaluator =
            new AuthenticationFreshnessEvaluator(fixedClock, Duration.ofMinutes(15));

    @Test
    void freshWithinWindowIsTrue() {
        assertThat(evaluator.isFresh(NOW.minus(Duration.ofMinutes(5)))).isTrue();
        assertThat(evaluator.isFresh(NOW.minus(Duration.ofMinutes(15)))).isTrue();
    }

    @Test
    void staleBeyondWindowIsFalse() {
        assertThat(evaluator.isFresh(NOW.minus(Duration.ofMinutes(16)))).isFalse();
        assertThat(evaluator.isFresh(NOW.minus(Duration.ofHours(1)))).isFalse();
    }

    @Test
    void nullTimestampIsFalse() {
        assertThat(evaluator.isFresh(null)).isFalse();
    }

    @Test
    void futureTimestampIsFalse() {
        // Defensive: if a caller supplies a future timestamp (clock skew or
        // malicious input), we must NOT treat it as fresh.
        assertThat(evaluator.isFresh(NOW.plus(Duration.ofMinutes(1)))).isFalse();
    }

    @Test
    void constructorRejectsInvalidWindow() {
        assertThatThrownBy(() -> new AuthenticationFreshnessEvaluator(fixedClock, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthenticationFreshnessEvaluator(fixedClock, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthenticationFreshnessEvaluator(null, Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}