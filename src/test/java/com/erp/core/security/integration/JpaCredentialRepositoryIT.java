package com.erp.core.security.integration;

import com.erp.core.security.SecurityModuleTestApplication;
import com.erp.core.security.internal.authentication.domain.Credential;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = SecurityModuleTestApplication.class)
class JpaCredentialRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("security_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("erp.security.persistence.type", () -> "jpa");
    }

    @Autowired
    private CredentialRepositoryPort repository;

    @Test
    void savesAndFindsCredential() {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        repository.save(new Credential(actor, tenant, "alice", "hash-1"));

        var found = repository.findByUsernameAndTenant(tenant, "alice");
        assertThat(found).isPresent();
        assertThat(found.get().actorId()).isEqualTo(actor);
        assertThat(found.get().passwordHash()).isEqualTo("hash-1");
    }

    @Test
    void tenantIsolation() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        repository.save(new Credential(UUID.randomUUID(), tenantA, "bob", "h"));

        assertThat(repository.findByUsernameAndTenant(tenantA, "bob")).isPresent();
        assertThat(repository.findByUsernameAndTenant(tenantB, "bob")).isEmpty();
    }
}