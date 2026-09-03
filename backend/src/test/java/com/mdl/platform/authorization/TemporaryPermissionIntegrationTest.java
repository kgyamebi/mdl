package com.mdl.platform.authorization;

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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class TemporaryPermissionIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_temp_permissions_test")
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
    private String receiverToken;

    @BeforeEach
    void login() throws Exception {
        ownerToken = login("owner@mdl.local", "Owner@123!");
        workerToken = login("john@mdl.local", "Worker@123!");
        receiverToken = login("receiver@mdl.local", "Receiver@123!");
    }

    @Test
    void workerCannotAccessMainWarehouseWithoutGrant() throws Exception {
        long mainLocationId = findLocationId(ownerToken, "LOC-MAIN");

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(mainLocationId))
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/locations")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='LOC-MAIN')]").doesNotExist());
    }

    @Test
    void ownerCanGrantTemporaryAccessToRestrictedWarehouse() throws Exception {
        long mainLocationId = findLocationId(ownerToken, "LOC-MAIN");
        long workerUserId = findUserId(ownerToken, "john@mdl.local");

        mockMvc.perform(post("/api/security/temporary-permissions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "permissionCode": "inventory:view",
                                  "locationId": %d,
                                  "reason": "Stock count support",
                                  "expiresAt": "%s"
                                }
                                """.formatted(workerUserId, mainLocationId, futureExpiry())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.locationCode").value("LOC-MAIN"));

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(mainLocationId))
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk());
    }

    @Test
    void importApprovalAutoGrantsReceiverAccess() throws Exception {
        long mainLocationId = findLocationId(ownerToken, "LOC-MAIN");
        long productId = findProductId(ownerToken, "MDL-LED-004");
        long receiverUserId = findUserId(ownerToken, "receiver@mdl.local");

        CreateImportRequest createRequest = new CreateImportRequest(
                "Test Supplier Ltd",
                "PO-TEMP-001",
                mainLocationId,
                null,
                "Task-scoped receive test",
                receiverUserId,
                List.of(new CreateImportRequest.CreateImportItemRequest(
                        productId, BigDecimal.valueOf(10), BigDecimal.valueOf(50), null)));

        MvcResult createResult = mockMvc.perform(post("/api/imports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long importId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        long itemId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(post("/api/imports/" + importId + "/submit")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/imports/" + importId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/security/temporary-permissions")
                        .param("userId", String.valueOf(receiverUserId))
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.referenceType=='IMPORT' && @.referenceId==" + importId + ")]")
                        .exists());

        ReceiveImportRequest receiveRequest = new ReceiveImportRequest(
                List.of(new ReceiveImportRequest.ReceiveImportItemRequest(
                        itemId, BigDecimal.valueOf(10), "Received OK")));

        mockMvc.perform(post("/api/imports/" + importId + "/receive")
                        .header("Authorization", "Bearer " + receiverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));

        mockMvc.perform(post("/api/imports/" + importId + "/verify")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/security/temporary-permissions")
                        .param("userId", String.valueOf(receiverUserId))
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.referenceType=='IMPORT' && @.referenceId==" + importId + ")]")
                        .doesNotExist());
    }

    @Test
    void receiverCannotReceiveWithoutApprovalGrant() throws Exception {
        long mainLocationId = findLocationId(ownerToken, "LOC-MAIN");
        long productId = findProductId(ownerToken, "MDL-LED-005");
        long receiverUserId = findUserId(ownerToken, "receiver@mdl.local");

        CreateImportRequest createRequest = new CreateImportRequest(
                "Pending Supplier",
                null,
                mainLocationId,
                null,
                null,
                receiverUserId,
                List.of(new CreateImportRequest.CreateImportItemRequest(
                        productId, BigDecimal.TEN, null, null)));

        MvcResult createResult = mockMvc.perform(post("/api/imports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long importId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        long itemId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(post("/api/imports/" + importId + "/submit")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        ReceiveImportRequest receiveRequest = new ReceiveImportRequest(
                List.of(new ReceiveImportRequest.ReceiveImportItemRequest(
                        itemId, BigDecimal.TEN, null)));

        mockMvc.perform(post("/api/imports/" + importId + "/receive")
                        .header("Authorization", "Bearer " + receiverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveRequest)))
                .andExpect(status().isForbidden());
    }

    private String futureExpiry() {
        return Instant.now().plus(7, ChronoUnit.DAYS).toString();
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

    private long findUserId(String token, String email) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("items");
        for (JsonNode item : items) {
            if (email.equalsIgnoreCase(item.path("email").asText())) {
                return item.path("id").asLong();
            }
        }
        throw new IllegalStateException("User not found: " + email);
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
