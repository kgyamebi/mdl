package com.mdl.platform.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.sales.dto.CreateSaleRequest;
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
class SaleIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_sales_test")
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
    void workerCanCompleteSaleAndManagerCanCancel() throws Exception {
        long shopId = findShopId(ownerToken, "SHOP-A");
        long shopWarehouseLocationId = findLocationId(ownerToken, "LOC-WH-A");
        long productId = findProductId(ownerToken, "MDL-LED-001");
        BigDecimal unitPrice = findProductPrice(ownerToken, "MDL-LED-001");

        MvcResult balanceBefore = mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(shopWarehouseLocationId))
                        .param("search", "MDL-LED-001")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andReturn();

        BigDecimal qtyBefore = new BigDecimal(objectMapper.readTree(balanceBefore.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("quantityOnHand").asText());

        CreateSaleRequest createRequest = new CreateSaleRequest(
                shopId,
                "Walk-in customer",
                "Counter sale",
                List.of(new CreateSaleRequest.CreateSaleItemRequest(
                        productId, BigDecimal.valueOf(2), null)),
                List.of(new CreateSaleRequest.CreateSalePaymentRequest(
                        "CASH", unitPrice.multiply(BigDecimal.valueOf(2)), null)));

        MvcResult createResult = mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.currencyCode").value("GHS"))
                .andExpect(jsonPath("$.data.saleNumber").exists())
                .andReturn();

        long saleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(shopWarehouseLocationId))
                        .param("search", "MDL-LED-001")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantityOnHand")
                        .value(qtyBefore.subtract(BigDecimal.valueOf(2)).doubleValue()));

        mockMvc.perform(get("/api/inventory/transactions")
                        .param("locationId", String.valueOf(shopWarehouseLocationId))
                        .param("productId", String.valueOf(productId))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.transactionType=='SALE')]").exists());

        mockMvc.perform(post("/api/sales/" + saleId + "/cancel")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Customer returned items same day" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(shopWarehouseLocationId))
                        .param("search", "MDL-LED-001")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantityOnHand").value(qtyBefore.doubleValue()));
    }

    @Test
    void workerCannotCancelSale() throws Exception {
        long shopId = findShopId(ownerToken, "SHOP-A");
        long productId = findProductId(ownerToken, "MDL-LED-002");
        BigDecimal unitPrice = findProductPrice(ownerToken, "MDL-LED-002");

        CreateSaleRequest createRequest = new CreateSaleRequest(
                shopId,
                null,
                null,
                List.of(new CreateSaleRequest.CreateSaleItemRequest(
                        productId, BigDecimal.ONE, null)),
                List.of(new CreateSaleRequest.CreateSalePaymentRequest(
                        "CASH", unitPrice, null)));

        MvcResult createResult = mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long saleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/sales/" + saleId + "/cancel")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Mistake" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotSellMoreThanAvailableStock() throws Exception {
        long shopId = findShopId(ownerToken, "SHOP-A");
        long productId = findProductId(ownerToken, "MDL-LED-003");

        CreateSaleRequest createRequest = new CreateSaleRequest(
                shopId,
                null,
                null,
                List.of(new CreateSaleRequest.CreateSaleItemRequest(
                        productId, BigDecimal.valueOf(99999), null)),
                List.of(new CreateSaleRequest.CreateSalePaymentRequest(
                        "CASH", BigDecimal.valueOf(99999), null)));

        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void paymentTotalMustMatchSaleTotal() throws Exception {
        long shopId = findShopId(ownerToken, "SHOP-A");
        long productId = findProductId(ownerToken, "MDL-LED-004");

        CreateSaleRequest createRequest = new CreateSaleRequest(
                shopId,
                null,
                null,
                List.of(new CreateSaleRequest.CreateSaleItemRequest(
                        productId, BigDecimal.ONE, null)),
                List.of(new CreateSaleRequest.CreateSalePaymentRequest(
                        "CASH", BigDecimal.ONE, null)));

        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict());
    }

    private long findShopId(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/shops")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        for (JsonNode item : items) {
            if (code.equals(item.path("code").asText())) {
                return item.path("id").asLong();
            }
        }
        throw new IllegalStateException("Shop not found: " + code);
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

    private BigDecimal findProductPrice(String token, String sku) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products")
                        .param("search", sku)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("items");
        for (JsonNode item : items) {
            if (sku.equals(item.path("sku").asText())) {
                return new BigDecimal(item.path("sellingPrice").asText());
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
