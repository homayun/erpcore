package com.erp.core.security.internal.authentication.application;

import com.erp.core.security.api.event.AuthenticationFailureReason;
import com.erp.core.security.internal.authentication.adapter.out.hashing.Argon2PasswordHasher;
import com.erp.core.security.internal.authentication.adapter.out.memory.InMemoryAccountLockoutAdapter;
import com.erp.core.security.internal.authentication.adapter.out.memory.InMemoryCredentialRepositoryAdapter;
import com.erp.core.security.internal.authentication.domain.Credential;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountLockoutEnforcementTest {

    private static final String PASSWORD = "CorrectPassword1!";

    @Test
    void accountIsLockedAfterRepeatedFailuresEvenWithCorrectPassword() {
        SecurityProperties props = new SecurityProperties(true, null, null, null, null, null, null, null, null, null);
        var hasher = Argon2PasswordHasher.productionDefaults();
        var repo = new InMemoryCredentialRepositoryAdapter();
        var lockout = new InMemoryAccountLockoutAdapter(props, Clock.systemUTC());
        var service = new LocalAuthenticationService(repo, hasher, lockout);

        UUID tenant = UUID.randomUUID();
        repo.save(new Credential(UUID.randomUUID(), tenant, "alice", hasher.hash(PASSWORD)));

        for (int i = 0; i < props.lockout().maxFailedAttempts(); i++) {
            var r = service.authenticate(new AuthenticationCommand(tenant, "alice", "wrong-" + i));
            assertThat(r).isInstanceOf(AuthenticationResult.Failure.class);
        }

        var locked = service.authenticate(new AuthenticationCommand(tenant, "alice", PASSWORD));
        assertThat(locked).isInstanceOf(AuthenticationResult.Failure.class);
        assertThat(((AuthenticationResult.Failure) locked).reason())
                .isEqualTo(AuthenticationFailureReason.ACCOUNT_LOCKED);
    }
}