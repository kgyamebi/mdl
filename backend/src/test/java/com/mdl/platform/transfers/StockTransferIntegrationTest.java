package com.mdl.platform.transfers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.transfers.dto.CreateStockTransferRequest;
import com.mdl.platform.transfers.dto.ReceiveStockTransferRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class StockTransferIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_transfers_test")
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
    void workerCanRequestOwnerCanDispatchAndWorkerReceives() throws Exception {
        long mainWarehouseId = findWarehouseId(ownerToken, "WH-MAIN");
        long shopAWarehouseId = findWarehouseId(ownerToken, "WH-SHOP-A");
        long shopALocationId = findLocationId(ownerToken, "LOC-WH-A");
        long productId = findProductId(ownerToken, "MDL-LED-001");

        MvcResult balanceBefore = mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(shopALocationId))
                        .param("search", "MDL-LED-001")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andReturn();

        BigDecimal qtyBefore = new BigDecimal(objectMapper.readTree(balanceBefore.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("quantityOnHand").asText());

        CreateStockTransferRequest createRequest = new CreateStockTransferRequest(
                mainWarehouseId,
                shopAWarehouseId,
                "Restock Shop A LED panels",
                List.of(new CreateStockTransferRequest.CreateStockTransferItemRequest(
                        productId, BigDecimal.valueOf(5), "Urgent restock")));

        MvcResult createResult = mockMvc.perform(post("/api/stock-transfers")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.transferNumber").exists())
                .andReturn();

        long transferId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        long itemId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(post("/api/stock-transfers/" + transferId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/stock-transfers/" + transferId + "/dispatch")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"));

        ReceiveStockTransferRequest receiveRequest = new ReceiveStockTransferRequest(
                List.of(new ReceiveStockTransferRequest.ReceiveStockTransferItemRequest(
                        itemId, BigDecimal.valueOf(5), "All units received")));

        mockMvc.perform(post("/api/stock-transfers/" + transferId + "/receive")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(shopALocationId))
                        .param("search", "MDL-LED-001")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantityOnHand")
                        .value(qtyBefore.add(BigDecimal.valueOf(5)).doubleValue()));
    }

    @Test
    void workerCannotDispatchTransfer() throws Exception {
        long mainWarehouseId = findWarehouseId(ownerToken, "WH-MAIN");
        long shopAWarehouseId = findWarehouseId(ownerToken, "WH-SHOP-A");
        long productId = findProductId(ownerToken, "MDL-LED-002");

        CreateStockTransferRequest createRequest = new CreateStockTransferRequest(
                mainWarehouseId,
                shopAWarehouseId,
                null,
                List.of(new CreateStockTransferRequest.CreateStockTransferItemRequest(
                        productId, BigDecimal.TEN, null)));

        MvcResult createResult = mockMvc.perform(post("/api/stock-transfers")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long transferId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/stock-transfers/" + transferId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/stock-transfers/" + transferId + "/dispatch")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotCreateTransferWithoutAuthorizedRoute() throws Exception {
        long shopAWarehouseId = findWarehouseId(ownerToken, "WH-SHOP-A");
        long shopBWarehouseId = findWarehouseId(ownerToken, "WH-SHOP-B");
        long productId = findProductId(ownerToken, "MDL-LED-003");

        CreateStockTransferRequest request = new CreateStockTransferRequest(
                shopBWarehouseId,
                shopAWarehouseId,
                null,
                List.of(new CreateStockTransferRequest.CreateStockTransferItemRequest(
                        productId, BigDecimal.TEN, null)));

        mockMvc.perform(post("/api/stock-transfers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
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
