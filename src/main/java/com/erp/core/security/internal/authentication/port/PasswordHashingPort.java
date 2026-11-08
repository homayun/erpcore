package com.erp.core.security.internal.authentication.port;

public interface PasswordHashingPort {

    /**
     * Hashes a raw password. Implementations MUST use a fresh, cryptographically
     * random salt on every call. Two calls with the same raw password MUST
     * return different hashes.
     */
    String hash(String rawPassword);

    /**
     * Constant-time comparison of a raw password against a previously generated hash.
     */
    boolean matches(String rawPassword, String encodedPassword);
}