package com.erp.core.security.internal.authentication.application;

import com.erp.core.security.api.event.AuthenticationFailureReason;
import com.erp.core.security.internal.authentication.adapter.out.hashing.Argon2PasswordHasher;
import com.erp.core.security.internal.authentication.adapter.out.memory.InMemoryCredentialRepositoryAdapter;
import com.erp.core.security.internal.authentication.domain.Credential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAuthenticationServiceTest {

    private final Argon2PasswordHasher hasher =
            new Argon2PasswordHasher(16, 32, 1, 4096, 1);
    private final InMemoryCredentialRepositoryAdapter repository =
            new InMemoryCredentialRepositoryAdapter();

    private LocalAuthenticationService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        repository.save(new Credential(actorId, tenantId, "alice",
                hasher.hash("CorrectPassword1!")));
        service = new LocalAuthenticationService(repository, hasher);
    }

    @Test
    void authenticatesCorrectCredentials() {
        AuthenticationResult result = service.authenticate(
                new AuthenticationCommand(tenantId, "alice", "CorrectPassword1!"));

        assertThat(result).isInstanceOf(AuthenticationResult.Success.class);
        AuthenticationResult.Success success = (AuthenticationResult.Success) result;
        assertThat(success.actorId()).isEqualTo(actorId);
        assertThat(success.tenantId()).isEqualTo(tenantId);
        assertThat(success.principal()).isEqualTo("alice");
    }

    @Test
    void rejectsWrongPassword() {
        AuthenticationResult result = service.authenticate(
                new AuthenticationCommand(tenantId, "alice", "WrongPassword!"));

        assertThat(result).isInstanceOf(AuthenticationResult.Failure.class);
        assertThat(((AuthenticationResult.Failure) result).reason())
                .isEqualTo(AuthenticationFailureReason.INVALID_CREDENTIALS);
    }

    @Test
    void rejectsUnknownUser() {
        AuthenticationResult result = service.authenticate(
                new AuthenticationCommand(tenantId, "ghost", "AnyPassword1!"));

        assertThat(result).isInstanceOf(AuthenticationResult.Failure.class);
        assertThat(((AuthenticationResult.Failure) result).reason())
                .isEqualTo(AuthenticationFailureReason.INVALID_CREDENTIALS);
    }

    @Test
    void unknownUserAndWrongPasswordProduceIdenticalFailureReason() {
        AuthenticationResult wrongPwd = service.authenticate(
                new AuthenticationCommand(tenantId, "alice", "WrongPassword!"));
        AuthenticationResult unknownUser = service.authenticate(
                new AuthenticationCommand(tenantId, "ghost", "AnyPassword1!"));

        assertThat(((AuthenticationResult.Failure) wrongPwd).reason())
                .isEqualTo(((AuthenticationResult.Failure) unknownUser).reason());
    }

    @Test
    void rejectsUserFromDifferentTenant() {
        UUID otherTenant = UUID.randomUUID();
        AuthenticationResult result = service.authenticate(
                new AuthenticationCommand(otherTenant, "alice", "CorrectPassword1!"));

        assertThat(result).isInstanceOf(AuthenticationResult.Failure.class);
    }
}