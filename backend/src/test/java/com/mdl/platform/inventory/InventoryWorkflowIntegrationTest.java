package com.mdl.platform.inventory;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class InventoryWorkflowIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_inventory_workflow_test")
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
    void workerCanSubmitAdjustmentRequestManagerCanApprove() throws Exception {
        long locationId = findLocationId(workerToken, "LOC-WH-A");
        long productId = findProductId(workerToken, "MDL-LED-002");

        String createBody = """
                {
                  "locationId": %d,
                  "productId": %d,
                  "requestedChange": -2,
                  "reason": "Two bulbs broken on shelf"
                }
                """.formatted(locationId, productId);

        MvcResult createResult = mockMvc.perform(post("/api/inventory/adjustment-requests")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        long requestId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/inventory/adjustment-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.resultTransactionId").exists());
    }

    @Test
    void managerCanReserveAndReleaseStock() throws Exception {
        long locationId = findLocationId(managerToken, "LOC-WH-A");
        long productId = findProductId(managerToken, "MDL-SWT-001");

        String reserveBody = """
                {
                  "locationId": %d,
                  "productId": %d,
                  "quantity": 5,
                  "notes": "Hold for customer pickup"
                }
                """.formatted(locationId, productId);

        MvcResult reserveResult = mockMvc.perform(post("/api/inventory/reservations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserveBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        long reservationId = objectMapper.readTree(reserveResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(locationId))
                        .param("search", "MDL-SWT-001")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantityReserved").value(5));

        mockMvc.perform(post("/api/inventory/reservations/" + reservationId + "/release")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RELEASED"));
    }

    @Test
    void workerCanReportDamage() throws Exception {
        long locationId = findLocationId(workerToken, "LOC-WH-A");
        long productId = findProductId(workerToken, "MDL-ACC-001");

        String damageBody = """
                {
                  "locationId": %d,
                  "productId": %d,
                  "quantity": 1,
                  "reason": "Water damage on tape roll"
                }
                """.formatted(locationId, productId);

        mockMvc.perform(post("/api/inventory/damage-reports")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(damageBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transactionType").value("DAMAGE"))
                .andExpect(jsonPath("$.data.quantityChange").value(-1));
    }

    @Test
    void summaryShowsLowStockAndPendingCounts() throws Exception {
        mockMvc.perform(get("/api/inventory/summary")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceRowCount").value(greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.data.lowStockCount").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void workerCannotApproveAdjustmentRequest() throws Exception {
        mockMvc.perform(post("/api/inventory/adjustment-requests/99999/approve")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private long findLocationId(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/locations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return findIdByCode(objectMapper.readTree(result.getResponse().getContentAsString()).path("data"), code);
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

    private long findIdByCode(JsonNode items, String code) {
        for (JsonNode item : items) {
            if (code.equals(item.path("code").asText())) {
                return item.path("id").asLong();
            }
        }
        throw new IllegalStateException("Location not found: " + code);
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
