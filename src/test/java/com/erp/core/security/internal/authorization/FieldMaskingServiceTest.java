package com.erp.core.security.internal.authorization;

import com.erp.core.security.internal.authorization.application.FieldMaskingService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldMaskingServiceTest {

    private final FieldMaskingService service = new FieldMaskingService();

    // --- REDACT ----------------------------------------------------------

    @Test
    void redactReplacesWithStars() {
        Object masked = service.mask("secret-value",
                FieldMaskingService.MaskingStrategy.REDACT);
        assertThat(masked).isEqualTo("***");
    }

    // --- PARTIAL ---------------------------------------------------------

    @Test
    void partialMaskKeepsEnds() {
        Object masked = service.mask("1234567890",
                FieldMaskingService.MaskingStrategy.PARTIAL);
        assertThat(masked.toString()).startsWith("12");
        assertThat(masked.toString()).endsWith("90");
        assertThat(masked.toString()).contains("*");
    }

    @Test
    void partialMaskShortValuesStaySafe() {
        // 2-char values become "**" — no plaintext leak
        assertThat(service.mask("ab",
                FieldMaskingService.MaskingStrategy.PARTIAL)).isEqualTo("**");
    }

    // --- HASH ------------------------------------------------------------

    @Test
    void hashIsDeterministic() {
        Object h1 = service.mask("secret",
                FieldMaskingService.MaskingStrategy.HASH);
        Object h2 = service.mask("secret",
                FieldMaskingService.MaskingStrategy.HASH);
        assertThat(h1).isEqualTo(h2);
        assertThat(h1.toString()).startsWith("sha256:");
    }

    @Test
    void hashIsIrreversibleAtFirstGlance() {
        Object hash = service.mask("1234567890",
                FieldMaskingService.MaskingStrategy.HASH);
        // Plaintext must not appear in the hash output
        assertThat(hash.toString()).doesNotContain("1234567890");
    }

    // --- HIDDEN ----------------------------------------------------------

    @Test
    void hiddenReturnsSentinelInstance() {
        Object masked = service.mask("x",
                FieldMaskingService.MaskingStrategy.HIDDEN);
        // Must be the same singleton — callers may compare by reference
        assertThat(masked).isSameAs(FieldMaskingService.HIDDEN);
    }

    // --- NULL ------------------------------------------------------------

    @Test
    void nullValuePassesThrough() {
        assertThat(service.mask(null,
                FieldMaskingService.MaskingStrategy.PARTIAL)).isNull();
        assertThat(service.mask(null,
                FieldMaskingService.MaskingStrategy.HASH)).isNull();
        assertThat(service.mask(null,
                FieldMaskingService.MaskingStrategy.REDACT)).isNull();
    }
}