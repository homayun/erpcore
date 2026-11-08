package com.erp.core.security.admin;

import com.erp.core.security.SecurityModuleTestApplication;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityModuleTestApplication.class)
@AutoConfigureMockMvc
class AdminAccessControlTest {

    private static final String ADMIN_TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String ADMIN_PASSWORD = "AdminPassword1!";
    private static final String NEW_USER_PASSWORD = "AliceStrongPwd1!";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",
                () -> "jdbc:h2:mem:admin_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        r.add("erp.security.enabled", () -> "true");
        r.add("erp.security.admin.bootstrap.enabled", () -> "true");
        r.add("erp.security.admin.bootstrap.tenant-id", () -> ADMIN_TENANT);
        r.add("erp.security.admin.bootstrap.username", () -> "admin");
        r.add("erp.security.admin.bootstrap.password", () -> ADMIN_PASSWORD);
    }

    @Autowired MockMvc mockMvc;

    private String loginAndGetJwt(String tenantId, String username, String password) throws Exception {
        var res = mockMvc.perform(post("/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + tenantId + "\",\"username\":\"" + username
                                + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(res.getResponse().getContentAsString(), "$.accessToken");
        assertThat(token).isNotBlank();
        return token;
    }

    @Test
    void adminCanListUsers() throws Exception {
        String jwt = loginAndGetJwt(ADMIN_TENANT, "admin", ADMIN_PASSWORD);
        mockMvc.perform(get("/admin/users")
                        .param("tenantId", ADMIN_TENANT)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    void adminWithoutJwtIsRejected() throws Exception {
        mockMvc.perform(get("/admin/users").param("tenantId", ADMIN_TENANT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateAnotherUser() throws Exception {
        String jwt = loginAndGetJwt(ADMIN_TENANT, "admin", ADMIN_PASSWORD);
        mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + ADMIN_TENANT
                                + "\",\"username\":\"alice\",\"password\":\"" + NEW_USER_PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void weakPasswordRejectedWith400() throws Exception {
        String jwt = loginAndGetJwt(ADMIN_TENANT, "admin", ADMIN_PASSWORD);
        mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + ADMIN_TENANT
                                + "\",\"username\":\"weakuser\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEC-4001"));
    }

    @Test
    void nonAdminGets403() throws Exception {
        String adminJwt = loginAndGetJwt(ADMIN_TENANT, "admin", ADMIN_PASSWORD);
        mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + ADMIN_TENANT
                                + "\",\"username\":\"bob\",\"password\":\"" + NEW_USER_PASSWORD + "\"}"))
                .andExpect(status().isCreated());

        String bobJwt = loginAndGetJwt(ADMIN_TENANT, "bob", NEW_USER_PASSWORD);
        mockMvc.perform(get("/admin/users")
                        .param("tenantId", ADMIN_TENANT)
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isForbidden());
    }
}