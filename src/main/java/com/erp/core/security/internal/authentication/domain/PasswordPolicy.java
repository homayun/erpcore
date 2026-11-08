package com.erp.core.security.internal.authentication.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Declarative password strength rules. Defaults follow NIST SP 800-63B and
 * OWASP ASVS 5.0 recommendations.
 *
 * This class does NOT hash. It only validates the raw password before hashing.
 */
public class PasswordPolicy {

    private final int minLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSpecial;

    public PasswordPolicy(
            int minLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireDigit,
            boolean requireSpecial
    ) {
        if (minLength < 8) {
            throw new IllegalArgumentException("minLength must be >= 8");
        }
        this.minLength = minLength;
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireDigit = requireDigit;
        this.requireSpecial = requireSpecial;
    }

    public static PasswordPolicy defaults() {
        return new PasswordPolicy(12, true, true, true, true);
    }

    public void validate(String rawPassword) {
        Objects.requireNonNull(rawPassword, "password must not be null");

        List<String> violations = new ArrayList<>();
        if (rawPassword.length() < minLength) {
            violations.add("length must be >= " + minLength);
        }
        if (requireUppercase && rawPassword.chars().noneMatch(Character::isUpperCase)) {
            violations.add("must contain an uppercase letter");
        }
        if (requireLowercase && rawPassword.chars().noneMatch(Character::isLowerCase)) {
            violations.add("must contain a lowercase letter");
        }
        if (requireDigit && rawPassword.chars().noneMatch(Character::isDigit)) {
            violations.add("must contain a digit");
        }
        if (requireSpecial && rawPassword.chars().allMatch(Character::isLetterOrDigit)) {
            violations.add("must contain a special character");
        }

        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolation(violations);
        }
    }
}