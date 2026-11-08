package com.erp.core.security.internal.authentication.adapter.out.memory;

import com.erp.core.security.internal.authentication.domain.Credential;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCredentialRepositoryAdapterTest {

    private final InMemoryCredentialRepositoryAdapter adapter =
            new InMemoryCredentialRepositoryAdapter();

    @Test
    void saveAndFindByUsernameAndTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Credential credential = new Credential(actorId, tenantId, "alice", "hash-1");

        adapter.save(credential);

        assertThat(adapter.findByUsernameAndTenant(tenantId, "alice"))
                .contains(credential);
    }

    @Test
    void findReturnsEmptyForDifferentTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        adapter.save(new Credential(UUID.randomUUID(), tenantA, "bob", "h"));

        assertThat(adapter.findByUsernameAndTenant(tenantB, "bob")).isEmpty();
    }

    @Test
    void findReturnsEmptyForUnknownUser() {
        assertThat(adapter.findByUsernameAndTenant(UUID.randomUUID(), "ghost")).isEmpty();
    }

    @Test
    void usernameLookupIsCaseInsensitive() {
        UUID tenantId = UUID.randomUUID();
        adapter.save(new Credential(UUID.randomUUID(), tenantId, "Alice", "h"));

        assertThat(adapter.findByUsernameAndTenant(tenantId, "alice")).isPresent();
        assertThat(adapter.findByUsernameAndTenant(tenantId, "ALICE")).isPresent();
    }

    @Test
    void updatePasswordHashAppliesNewHash() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        adapter.save(new Credential(actorId, tenantId, "carol", "old-hash"));

        adapter.updatePasswordHash(actorId, "new-hash");

        assertThat(adapter.findByUsernameAndTenant(tenantId, "carol"))
                .get()
                .extracting(Credential::passwordHash)
                .isEqualTo("new-hash");
    }
}