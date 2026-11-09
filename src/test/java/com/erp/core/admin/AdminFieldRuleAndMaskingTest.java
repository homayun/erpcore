package com.erp.core.admin;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the full BOLA-prevention chain end-to-end:
 *   1. Field rule CRUD works via /admin/field-rules
 *   2. Inserting a rule actually changes what /admin/demo/invoice returns
 *   3. Deleting the rule restores the plaintext field
 */
@SpringBootTest(classes = SecurityModuleTestApplication.class)
@AutoConfigureMockMvc
class AdminFieldRuleAndMaskingTest {

    private static final String ADMIN_TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String ADMIN_PASSWORD = "AdminPassword1!";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",
                () -> "jdbc:h2:mem:admin_field_rule_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        r.add("erp.security.enabled", () -> "true");
        r.add("erp.security.admin.bootstrap.enabled", () -> "true");
        r.add("erp.security.admin.bootstrap.tenant-id", () -> ADMIN_TENANT);
        r.add("erp.security.admin.bootstrap.username", () -> "admin");
        r.add("erp.security.admin.bootstrap.password", () -> ADMIN_PASSWORD);
    }

    @Autowired MockMvc mockMvc;

    private String adminJwt() throws Exception {
        var res = mockMvc.perform(post("/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + ADMIN_TENANT
                                + "\",\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void fieldRuleCrudAndMaskingLifecycle() throws Exception {
        String jwt = adminJwt();

        // 1) No rule yet -> amount visible as raw number
        mockMvc.perform(get("/admin/demo/invoice")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.invoiceNo").value("INV-2026-0001"));

        // 2) Create the rule
        mockMvc.perform(post("/admin/field-rules")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\":\"INVOICE\",\"fieldName\":\"amount\"," +
                                 "\"requiredPermission\":\"VIEW_INVOICE_AMOUNT\"," +
                                 "\"maskingWhenDenied\":\"REDACT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceType").value("INVOICE"))
                .andExpect(jsonPath("$.fieldName").value("amount"))
                .andExpect(jsonPath("$.maskingWhenDenied").value("REDACT"));

        // 3) Amount is now masked (admin lacks VIEW_INVOICE_AMOUNT)
        mockMvc.perform(get("/admin/demo/invoice")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value("***"))
                .andExpect(jsonPath("$.invoiceNo").value("INV-2026-0001"))
                .andExpect(jsonPath("$.customerName").value("Acme Corp"));

        // 4) List rules
        mockMvc.perform(get("/admin/field-rules")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fieldName").value("amount"));

        // 5) Delete the rule -> amount visible again
        mockMvc.perform(delete("/admin/field-rules/INVOICE/amount")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/admin/demo/invoice")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(50000));
    }

    @Test
    void invalidMaskingStrategyRejected() throws Exception {
        String jwt = adminJwt();
        mockMvc.perform(post("/admin/field-rules")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\":\"INVOICE\",\"fieldName\":\"x\"," +
                                 "\"requiredPermission\":\"VIEW_X\"," +
                                 "\"maskingWhenDenied\":\"NONSENSE\"}"))
                .andExpect(status().isBadRequest());
    }
}