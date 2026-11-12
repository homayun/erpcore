package com.erp.core.security.web;

import com.erp.core.security.internal.authentication.adapter.out.hashing.Argon2PasswordHasher;
import com.erp.core.security.internal.authentication.domain.Credential;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import com.erp.core.security.internal.authentication.port.PasswordHashingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.erp.core.security.SecurityModuleTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "erp.security.enabled=true",
        "spring.main.banner-mode=off"
})
class SecurityFilterChainIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CredentialRepositoryPort credentialRepository;
    @Autowired PasswordHashingPort passwordHasher;
    @Autowired JdbcTemplate jdbcTemplate;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private static final String PASSWORD = "CorrectPassword1!";

    @BeforeEach
    void seedUser() {
        ensureTenantExists(tenantId);
        credentialRepository.save(new Credential(actorId, tenantId, "alice",
                passwordHasher.hash(PASSWORD)));
    }
    /**
     * V17 FK {@code fk_credentials_tenant} requires {@code sec_credentials.tenant_id}
     * to reference an existing {@code sec_tenant.id}. Tests use a random tenant
     * per test method, so seed a matching tenant row idempotently.
     *
     * The insert is portable across H2 and PostgreSQL (INSERT ... SELECT ... WHERE NOT EXISTS).
     * Code is derived from the full tenant UUID to guarantee uniqueness.
     */
    private void ensureTenantExists(UUID tenantId) {
        jdbcTemplate.update(
                "INSERT INTO sec_tenant (id, code, name, status, created_at) " +
                "SELECT ?, ?, ?, ?, CURRENT_TIMESTAMP " +
                "WHERE NOT EXISTS (SELECT 1 FROM sec_tenant WHERE id = ?)",
                tenantId,
                "TEST_" + tenantId.toString().toUpperCase(),
                "Test Tenant",
                "ACTIVE",
                tenantId);
    }

    @Test
    void headersAppliedToUnauthenticatedResponse() throws Exception {
        mockMvc.perform(get("/api/something"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void unauthorizedReturnsJsonErrorCode() throws Exception {
        mockMvc.perform(get("/api/something"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SEC-4010"));
    }

    @Test
    void loginWithoutCsrfRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + tenantId + "\",\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithValidCredentialsSucceeds() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + tenantId + "\",\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorId").value(actorId.toString()))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + tenantId + "\",\"username\":\"alice\",\"password\":\"WrongPwd!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfEndpointReturnsToken() throws Exception {
        mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").exists())
                .andExpect(jsonPath("$.token").exists());
    }
}