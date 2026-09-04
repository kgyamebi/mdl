package com.mdl.platform.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.support.DockerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end RBAC checks for owner, shop manager, and shop worker demo accounts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class RoleAccessIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_role_access_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String ownerToken;
    private String managerToken;
    private String workerToken;

    @BeforeEach
    void login() throws Exception {
        ownerToken = login("owner@mdl.local", "Owner@123!");
        managerToken = login("michael@mdl.local", "Manager@123!");
        workerToken = login("john@mdl.local", "Worker@123!");
    }

    @Test
    void ownerCanAccessAdminAndOperationalEndpoints() throws Exception {
        expectOk(ownerToken, "/api/users");
        expectOk(ownerToken, "/api/business/structure");
        expectOk(ownerToken, "/api/reports/sales-summary/export");
        expectOk(ownerToken, "/api/reports/sales-summary/export/pdf");
        expectOk(ownerToken, "/api/stock-transfers/form-options");
        expectOk(ownerToken, "/api/inventory/balances");
    }

    @Test
    void shopManagerCanRunShopOperationsAndReports() throws Exception {
        expectOk(managerToken, "/api/inventory/balances");
        expectOk(managerToken, "/api/locations");
        expectOk(managerToken, "/api/products");
        expectOk(managerToken, "/api/sales");
        expectOk(managerToken, "/api/stock-transfers");
        expectOk(managerToken, "/api/stock-transfers/form-options");
        expectOk(managerToken, "/api/reports/exports");
        expectOk(managerToken, "/api/reports/sales-summary/export");
        expectOk(managerToken, "/api/reports/sales-summary/export/pdf");
        expectOk(managerToken, "/api/approvals/inbox");
        expectOk(managerToken, "/api/audit-logs");

        expectForbidden(managerToken, "/api/users");
    }

    @Test
    void shopWorkerCanUseFloorToolsButNotAdminOrReports() throws Exception {
        expectOk(workerToken, "/api/inventory/balances");
        expectOk(workerToken, "/api/products");
        expectOk(workerToken, "/api/sales");
        expectOk(workerToken, "/api/stock-transfers");
        expectOk(workerToken, "/api/stock-transfers/form-options");

        expectForbidden(workerToken, "/api/reports/sales-summary/export");
        expectForbidden(workerToken, "/api/reports/sales-summary/export/pdf");
        expectForbidden(workerToken, "/api/users");
        expectForbidden(workerToken, "/api/business/structure");
    }

    @Test
    void eachRoleGetsExpectedPermissionsInLoginProfile() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isArray());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions", hasItem("inventory:view")))
                .andExpect(jsonPath("$.data.permissions", hasItem("report:export")));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions", hasItem("sale:create")))
                .andExpect(jsonPath("$.data.permissions", hasItem("transfer:view")));
    }

    private void expectOk(String token, String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void expectForbidden(String token, String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
