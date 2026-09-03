package com.mdl.platform.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.imports.dto.CreateImportRequest;
import com.mdl.platform.imports.dto.ReceiveImportRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class ImportIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_imports_test")
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
    void ownerCanCreateApproveReceiveAndVerifyImport() throws Exception {
        long mainLocationId = findLocationId(ownerToken, "LOC-MAIN");
        long productId = findProductId(ownerToken, "MDL-LED-001");

        MvcResult balanceBefore = mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(mainLocationId))
                        .param("search", "MDL-LED-001")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();

        BigDecimal qtyBefore = new BigDecimal(objectMapper.readTree(balanceBefore.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("quantityOnHand").asText());

        CreateImportRequest createRequest = new CreateImportRequest(
                "Shenzhen LED Supplies Co.",
                "PO-2026-8842",
                mainLocationId,
                null,
                "Container shipment from China",
                null,
                List.of(new CreateImportRequest.CreateImportItemRequest(
                        productId,
                        BigDecimal.valueOf(50),
                        BigDecimal.valueOf(140),
                        "48W panels")));

        MvcResult createResult = mockMvc.perform(post("/api/imports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.importNumber").exists())
                .andReturn();

        long importId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        long itemId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(post("/api/imports/" + importId + "/submit")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        mockMvc.perform(post("/api/imports/" + importId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        ReceiveImportRequest receiveRequest = new ReceiveImportRequest(
                List.of(new ReceiveImportRequest.ReceiveImportItemRequest(
                        itemId, BigDecimal.valueOf(50), "All cartons intact")));

        mockMvc.perform(post("/api/imports/" + importId + "/receive")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.items[0].receivedQuantity").value(50));

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(mainLocationId))
                        .param("search", "MDL-LED-001")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantityOnHand")
                        .value(qtyBefore.add(BigDecimal.valueOf(50)).doubleValue()));

        mockMvc.perform(post("/api/imports/" + importId + "/verify")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));

        mockMvc.perform(get("/api/inventory/transactions")
                        .param("locationId", String.valueOf(mainLocationId))
                        .param("productId", String.valueOf(productId))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.transactionType=='IMPORT_RECEIVE')]").exists());
    }

    @Test
    void workerCannotCreateImport() throws Exception {
        long mainLocationId = findLocationId(ownerToken, "LOC-MAIN");
        long productId = findProductId(ownerToken, "MDL-LED-002");

        CreateImportRequest request = new CreateImportRequest(
                "Test Supplier",
                null,
                mainLocationId,
                null,
                null,
                null,
                List.of(new CreateImportRequest.CreateImportItemRequest(
                        productId, BigDecimal.TEN, null, null)));

        mockMvc.perform(post("/api/imports")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotCreateImportToShopWarehouse() throws Exception {
        long shopWarehouseLocationId = findLocationId(ownerToken, "LOC-WH-A");
        long productId = findProductId(ownerToken, "MDL-LED-002");

        CreateImportRequest request = new CreateImportRequest(
                "Test Supplier",
                null,
                shopWarehouseLocationId,
                null,
                null,
                null,
                List.of(new CreateImportRequest.CreateImportItemRequest(
                        productId, BigDecimal.TEN, null, null)));

        mockMvc.perform(post("/api/imports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerCanAddImportEvidence() throws Exception {
        long mainLocationId = findLocationId(ownerToken, "LOC-MAIN");
        long productId = findProductId(ownerToken, "MDL-LED-003");

        CreateImportRequest createRequest = new CreateImportRequest(
                "Guangzhou Electric Ltd",
                "INV-99",
                mainLocationId,
                null,
                null,
                null,
                List.of(new CreateImportRequest.CreateImportItemRequest(
                        productId, BigDecimal.valueOf(20), null, null)));

        MvcResult createResult = mockMvc.perform(post("/api/imports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long importId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/imports/" + importId + "/evidence")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "evidenceType": "NOTE",
                                  "description": "Bill of lading received via email"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/imports/" + importId + "/evidence")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(greaterThanOrEqualTo(1))));
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
