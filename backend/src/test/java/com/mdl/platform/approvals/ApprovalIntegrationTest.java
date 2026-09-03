package com.mdl.platform.approvals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.transfers.dto.CreateStockTransferRequest;
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

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class ApprovalIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_approvals_test")
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
    private String workerToken;
    private String managerToken;

    @BeforeEach
    void login() throws Exception {
        ownerToken = login("owner@mdl.local", "Owner@123!");
        workerToken = login("john@mdl.local", "Worker@123!");
        managerToken = login("michael@mdl.local", "Manager@123!");
    }

    @Test
    void ownerCanListDefaultApprovalRules() throws Exception {
        mockMvc.perform(get("/api/approvals/rules")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.data[?(@.code == 'ADJ-DEFAULT')].steps.length()").value(1));
    }

    @Test
    void thresholdRoutingSelectsRuleByQuantity() throws Exception {
        MvcResult createRuleResult = mockMvc.perform(post("/api/approvals/rules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ADJ-HIGH",
                                  "name": "Large adjustment approval",
                                  "entityType": "INVENTORY_ADJUSTMENT",
                                  "requiredPermission": "inventory:adjust",
                                  "minAbsQuantity": 10,
                                  "enabled": true,
                                  "priority": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long highRuleId = objectMapper.readTree(createRuleResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(put("/api/approvals/rules/" + highRuleId + "/steps")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "stepOrder": 1,
                                    "name": "Manager review",
                                    "requiredPermission": "inventory:adjust"
                                  },
                                  {
                                    "stepOrder": 2,
                                    "name": "Owner sign-off",
                                    "requiredPermission": "approval:manage"
                                  }
                                ]
                                """))
                .andExpect(status().isOk());

        long locationId = findLocationId(workerToken, "LOC-WH-A");
        long productId = findProductId(workerToken, "MDL-LED-002");

        MvcResult smallResult = mockMvc.perform(post("/api/inventory/adjustment-requests")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationId": %d,
                                  "productId": %d,
                                  "requestedChange": -2,
                                  "reason": "Small adjustment"
                                }
                                """.formatted(locationId, productId)))
                .andExpect(status().isCreated())
                .andReturn();

        long smallRequestId = objectMapper.readTree(smallResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/approvals/instances/INVENTORY_ADJUSTMENT/" + smallRequestId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalRuleCode").value("ADJ-DEFAULT"));

        MvcResult largeResult = mockMvc.perform(post("/api/inventory/adjustment-requests")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationId": %d,
                                  "productId": %d,
                                  "requestedChange": -12,
                                  "reason": "Large adjustment"
                                }
                                """.formatted(locationId, productId)))
                .andExpect(status().isCreated())
                .andReturn();

        long largeRequestId = objectMapper.readTree(largeResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/approvals/instances/INVENTORY_ADJUSTMENT/" + largeRequestId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalRuleCode").value("ADJ-HIGH"))
                .andExpect(jsonPath("$.data.totalSteps").value(2));
    }

    @Test
    void parallelStepAllowsAnyEligibleApprover() throws Exception {
        long adjRuleId = findRuleId(ownerToken, "ADJ-DEFAULT");

        mockMvc.perform(put("/api/approvals/rules/" + adjRuleId + "/steps")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "stepOrder": 1,
                                    "name": "Manager or owner review",
                                    "requiredPermission": "inventory:adjust"
                                  },
                                  {
                                    "stepOrder": 1,
                                    "name": "Manager or owner review",
                                    "requiredPermission": "approval:manage"
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps.length()").value(2));

        long locationId = findLocationId(workerToken, "LOC-WH-A");
        long productId = findProductId(workerToken, "MDL-LED-002");

        MvcResult createResult = mockMvc.perform(post("/api/inventory/adjustment-requests")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationId": %d,
                                  "productId": %d,
                                  "requestedChange": -1,
                                  "reason": "Parallel approval test"
                                }
                                """.formatted(locationId, productId)))
                .andExpect(status().isCreated())
                .andReturn();

        long requestId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/approvals/instances/INVENTORY_ADJUSTMENT/" + requestId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parallelStep").value(true))
                .andExpect(jsonPath("$.data.currentStepPermissions.length()").value(2))
                .andExpect(jsonPath("$.data.totalSteps").value(1));

        mockMvc.perform(post("/api/inventory/adjustment-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.resultTransactionId").exists());
    }

    @Test
    void duplicateParallelStepPermissionIsRejected() throws Exception {
        long adjRuleId = findRuleId(ownerToken, "ADJ-DEFAULT");

        mockMvc.perform(put("/api/approvals/rules/" + adjRuleId + "/steps")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "stepOrder": 1,
                                    "name": "Manager review",
                                    "requiredPermission": "inventory:adjust"
                                  },
                                  {
                                    "stepOrder": 1,
                                    "name": "Duplicate manager review",
                                    "requiredPermission": "inventory:adjust"
                                  }
                                ]
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void multiStepAdjustmentRequiresSequentialApprovers() throws Exception {
        long adjRuleId = findRuleId(ownerToken, "ADJ-DEFAULT");

        mockMvc.perform(put("/api/approvals/rules/" + adjRuleId + "/steps")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "stepOrder": 1,
                                    "name": "Shop manager review",
                                    "requiredPermission": "inventory:adjust"
                                  },
                                  {
                                    "stepOrder": 2,
                                    "name": "Owner sign-off",
                                    "requiredPermission": "approval:manage"
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps.length()").value(2));

        long locationId = findLocationId(workerToken, "LOC-WH-A");
        long productId = findProductId(workerToken, "MDL-LED-002");

        MvcResult createResult = mockMvc.perform(post("/api/inventory/adjustment-requests")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationId": %d,
                                  "productId": %d,
                                  "requestedChange": -1,
                                  "reason": "Multi-step approval test"
                                }
                                """.formatted(locationId, productId)))
                .andExpect(status().isCreated())
                .andReturn();

        long requestId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/approvals/instances/INVENTORY_ADJUSTMENT/" + requestId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.currentStepOrder").value(1))
                .andExpect(jsonPath("$.data.totalSteps").value(2));

        mockMvc.perform(post("/api/inventory/adjustment-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(get("/api/approvals/instances/INVENTORY_ADJUSTMENT/" + requestId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentStepOrder").value(2))
                .andExpect(jsonPath("$.data.actions.length()").value(1));

        mockMvc.perform(post("/api/inventory/adjustment-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/inventory/adjustment-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.resultTransactionId").exists());
    }

    @Test
    void pendingAdjustmentAppearsInManagerInbox() throws Exception {
        long locationId = findLocationId(workerToken, "LOC-WH-A");
        long productId = findProductId(workerToken, "MDL-LED-002");

        mockMvc.perform(post("/api/inventory/adjustment-requests")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationId": %d,
                                  "productId": %d,
                                  "requestedChange": -1,
                                  "reason": "Approval inbox test"
                                }
                                """.formatted(locationId, productId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/approvals/inbox")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.adjustmentCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[?(@.entityType == 'INVENTORY_ADJUSTMENT')]").exists());
    }

    @Test
    void transferRequestStartsApprovalWorkflow() throws Exception {
        long mainWarehouseId = findWarehouseId(ownerToken, "WH-MAIN");
        long shopWarehouseId = findWarehouseId(ownerToken, "WH-SHOP-A");
        long productId = findProductId(workerToken, "MDL-LED-001");

        CreateStockTransferRequest createRequest = new CreateStockTransferRequest(
                mainWarehouseId,
                shopWarehouseId,
                "Workflow integration test",
                List.of(new CreateStockTransferRequest.CreateStockTransferItemRequest(
                        productId, BigDecimal.valueOf(3), null)));

        MvcResult createResult = mockMvc.perform(post("/api/stock-transfers")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andReturn();

        long transferId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/approvals/instances/STOCK_TRANSFER/" + transferId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityType").value("STOCK_TRANSFER"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.approvalRuleCode").value("XFER-DEFAULT"));
    }

    @Test
    void workerCannotViewApprovalInbox() throws Exception {
        mockMvc.perform(get("/api/approvals/inbox")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanCreateAndUpdateApprovalRule() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/approvals/rules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ADJ-LARGE",
                                  "name": "Large adjustment rule",
                                  "description": "High quantity adjustments",
                                  "entityType": "INVENTORY_ADJUSTMENT",
                                  "requiredPermission": "inventory:adjust",
                                  "minAbsQuantity": 50,
                                  "enabled": true,
                                  "priority": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("ADJ-LARGE"))
                .andReturn();

        long ruleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(patch("/api/approvals/rules/" + ruleId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false,
                                  "priority": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.priority").value(20));
    }

    private long findRuleId(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/approvals/rules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode item : objectMapper.readTree(result.getResponse().getContentAsString()).path("data")) {
            if (code.equals(item.path("code").asText())) {
                return item.path("id").asLong();
            }
        }
        throw new IllegalStateException("Rule not found: " + code);
    }

    private long findWarehouseId(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/warehouses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        for (JsonNode item : items) {
            if (code.equals(item.path("code").asText())) {
                return item.path("id").asLong();
            }
        }
        throw new IllegalStateException("Warehouse not found: " + code);
    }

    private long findLocationId(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/locations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        for (JsonNode item : items) {
            if (code.equals(item.path("code").asText())) {
                return item.path("id").asLong();
            }
        }
        throw new IllegalStateException("Location not found: " + code);
    }

    private long findProductId(String token, String sku) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products")
                        .param("search", sku)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("items");
        for (JsonNode item : items) {
            if (sku.equals(item.path("sku").asText())) {
                return item.path("id").asLong();
            }
        }
        throw new IllegalStateException("Product not found: " + sku);
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
