package com.erp.core.security.internal.authentication.domain;

import java.util.List;

public class PasswordPolicyViolation extends RuntimeException {

    private final List<String> violations;

    public PasswordPolicyViolation(List<String> violations) {
        super("Password does not satisfy policy: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}