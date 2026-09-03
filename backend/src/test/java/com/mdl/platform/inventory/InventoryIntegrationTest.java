package com.mdl.platform.inventory;

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
class InventoryIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_inventory_test")
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

    @BeforeEach
    void login() throws Exception {
        ownerToken = login("owner@mdl.local", "Owner@123!");
        workerToken = login("john@mdl.local", "Worker@123!");
    }

    @Test
    void ownerSeesMainWarehouseStock() throws Exception {
        MvcResult locations = mockMvc.perform(get("/api/locations")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();

        long mainLocationId = findLocationIdByCode(locations, "LOC-MAIN");

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(mainLocationId))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(6)));
    }

    @Test
    void workerSeesOnlyAssignedLocationBalances() throws Exception {
        mockMvc.perform(get("/api/inventory/balances")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.data.items[?(@.locationCode=='LOC-MAIN')]").doesNotExist());
    }

    @Test
    void workerCannotViewMainWarehouseBalances() throws Exception {
        MvcResult locations = mockMvc.perform(get("/api/locations")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();

        long mainLocationId = findLocationIdByCode(locations, "LOC-MAIN");

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(mainLocationId))
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void lowStockFilterFindsShopATape() throws Exception {
        mockMvc.perform(get("/api/inventory/balances")
                        .param("lowStockOnly", "true")
                        .param("search", "Tape")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].belowReorderLevel").value(true));
    }

    @Test
    void ownerCanListTransactionHistory() throws Exception {
        mockMvc.perform(get("/api/inventory/transactions")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.data.items[0].transactionType").value("OPENING_BALANCE"));
    }

    @Test
    void workerCannotPostAdjustment() throws Exception {
        String body = """
                {
                  "locationId": 1,
                  "productId": 1,
                  "quantityChange": 5
                }
                """;

        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    private long findLocationIdByCode(MvcResult locationsResult, String code) throws Exception {
        var items = objectMapper.readTree(locationsResult.getResponse().getContentAsString()).path("data");
        for (var item : items) {
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
