package com.erp.core.security.internal.authentication.adapter.out.hashing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Argon2PasswordHasherTest {

    // Lower-than-production cost for fast unit tests. Production bean uses
    // Argon2PasswordHasher.productionDefaults() â€” see SecurityModuleAutoConfiguration.
    private final Argon2PasswordHasher hasher =
            new Argon2PasswordHasher(16, 32, 1, 4096, 1);

    @Test
    void hashesProduceDifferentOutputsForSameInput() {
        String a = hasher.hash("SamePassword1!");
        String b = hasher.hash("SamePassword1!");
        assertThat(a).isNotEqualTo(b); // different salt each time
    }

    @Test
    void matchesAcceptsCorrectPassword() {
        String hash = hasher.hash("CorrectHorseBattery!");
        assertThat(hasher.matches("CorrectHorseBattery!", hash)).isTrue();
    }

    @Test
    void matchesRejectsWrongPassword() {
        String hash = hasher.hash("CorrectHorseBattery!");
        assertThat(hasher.matches("WrongPassword!", hash)).isFalse();
    }

    @Test
    void matchesRejectsNullOrInvalidInput() {
        String hash = hasher.hash("AnyPassword1!");
        assertThat(hasher.matches(null, hash)).isFalse();
        assertThat(hasher.matches("AnyPassword1!", null)).isFalse();
        assertThat(hasher.matches("AnyPassword1!", "not-a-valid-hash")).isFalse();
    }
}