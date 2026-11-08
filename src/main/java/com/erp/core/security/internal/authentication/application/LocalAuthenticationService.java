package com.erp.core.security.internal.authentication.application;

import com.erp.core.security.api.event.AuthenticationFailureReason;
import com.erp.core.security.internal.authentication.domain.Credential;
import com.erp.core.security.internal.authentication.port.AccountLockoutPort;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import com.erp.core.security.internal.authentication.port.PasswordHashingPort;

import java.util.Optional;
import java.util.UUID;

/**
 * Verifies credentials against the configured repository and hasher.
 *
 * Brute-force defence:
 * - Locked accounts are rejected BEFORE the real hash is compared; a dummy
 *   hash comparison keeps timing similar to a normal failure.
 * - Every wrong password calls AccountLockoutPort.recordFailure(actorId).
 *   (Success is recorded by the caller, see LoginController.)
 *
 * Timing-side-channel defence: when the username is unknown, a dummy hash
 * comparison is performed so response time is similar for "wrong password"
 * and "unknown user".
 *
 * The HTTP layer must still answer with one generic 401 for all failures.
 */
public class LocalAuthenticationService {

    private final String dummyHash;

    private final CredentialRepositoryPort credentialRepository;
    private final PasswordHashingPort passwordHasher;
    private final AccountLockoutPort lockout;

    public LocalAuthenticationService(
            CredentialRepositoryPort credentialRepository,
            PasswordHashingPort passwordHasher,
            AccountLockoutPort lockout
    ) {
        if (lockout == null) throw new IllegalArgumentException("lockout is required");
        this.credentialRepository = credentialRepository;
        this.passwordHasher = passwordHasher;
        this.lockout = lockout;
        this.dummyHash = passwordHasher.hash("__dummy_password_for_timing_protection__");
    }

    /**
     * @deprecated no lockout - kept only so legacy unit tests compile.
     * Production wiring uses the 3-argument constructor.
     */
    @Deprecated
    public LocalAuthenticationService(
            CredentialRepositoryPort credentialRepository,
            PasswordHashingPort passwordHasher
    ) {
        this(credentialRepository, passwordHasher, NoOpLockout.INSTANCE);
    }

    public AuthenticationResult authenticate(AuthenticationCommand command) {
        Optional<Credential> credential =
                credentialRepository.findByUsernameAndTenant(command.tenantId(), command.username());

        if (credential.isEmpty()) {
            passwordHasher.matches(command.password(), dummyHash);
            return new AuthenticationResult.Failure(AuthenticationFailureReason.INVALID_CREDENTIALS);
        }

        Credential stored = credential.get();

        if (lockout.isLocked(stored.actorId())) {
            passwordHasher.matches(command.password(), dummyHash);
            return new AuthenticationResult.Failure(AuthenticationFailureReason.ACCOUNT_LOCKED);
        }

        if (!passwordHasher.matches(command.password(), stored.passwordHash())) {
            lockout.recordFailure(stored.actorId());
            return new AuthenticationResult.Failure(AuthenticationFailureReason.INVALID_CREDENTIALS);
        }

        return new AuthenticationResult.Success(
                stored.actorId(),
                stored.tenantId(),
                stored.username()
        );
    }

    private static final class NoOpLockout implements AccountLockoutPort {
        static final NoOpLockout INSTANCE = new NoOpLockout();
        @Override public boolean isLocked(UUID actorId) { return false; }
        @Override public void recordFailure(UUID actorId) { }
        @Override public void recordSuccess(UUID actorId) { }
    }
}