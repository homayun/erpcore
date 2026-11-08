package com.erp.core.security.api.authorization;

public record ActionDescriptor(String verb, String scope) {
    public ActionDescriptor {
        if (verb == null || verb.isBlank()) {
            throw new IllegalArgumentException("Action verb is required");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("Action scope is required");
        }
    }

    public String toCanonicalString() {
        return scope.toUpperCase() + "_" + verb.toUpperCase();
    }
}