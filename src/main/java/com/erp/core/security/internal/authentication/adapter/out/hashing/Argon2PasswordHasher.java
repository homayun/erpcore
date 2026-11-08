package com.erp.core.security.internal.authentication.adapter.out.hashing;

import com.erp.core.security.internal.authentication.port.PasswordHashingPort;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.Objects;

/**
 * Argon2id password hasher (OWASP-recommended for new systems).
 *
 * Parameters are configurable so that tests can choose fast settings while
 * production uses stronger ones. Defaults match OWASP minimum for Argon2id:
 * memory=19 MB, iterations=2, parallelism=1.
 */
public class Argon2PasswordHasher implements PasswordHashingPort {

    private final Argon2PasswordEncoder encoder;

    public Argon2PasswordHasher(
            int saltLength,
            int hashLength,
            int parallelism,
            int memoryKb,
            int iterations
    ) {
        if (saltLength < 8) throw new IllegalArgumentException("saltLength must be >= 8");
        if (hashLength < 16) throw new IllegalArgumentException("hashLength must be >= 16");
        if (parallelism < 1) throw new IllegalArgumentException("parallelism must be >= 1");
        if (memoryKb < 1024) throw new IllegalArgumentException("memoryKb must be >= 1024");
        if (iterations < 1) throw new IllegalArgumentException("iterations must be >= 1");
        this.encoder = new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memoryKb, iterations);
    }

    public static Argon2PasswordHasher productionDefaults() {
        return new Argon2PasswordHasher(16, 32, 1, 19456, 2);
    }

    @Override
    public String hash(String rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}