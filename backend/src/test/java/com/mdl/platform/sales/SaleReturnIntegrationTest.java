package com.mdl.platform.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.sales.dto.CreateSaleRequest;
import com.mdl.platform.sales.dto.CreateSaleReturnRequest;
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
class SaleReturnIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_sale_returns_test")
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
    void managerCanProcessPartialReturn() throws Exception {
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
                "Return test customer",
                null,
                List.of(new CreateSaleRequest.CreateSaleItemRequest(
                        productId, BigDecimal.valueOf(2), null)),
                List.of(new CreateSaleRequest.CreateSalePaymentRequest(
                        "CASH", unitPrice.multiply(BigDecimal.valueOf(2)), null)));

        MvcResult createResult = mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode saleData = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        long saleId = saleData.path("id").asLong();
        long saleItemId = saleData.path("items").get(0).path("id").asLong();

        CreateSaleReturnRequest returnRequest = new CreateSaleReturnRequest(
                "DEFECTIVE",
                "One unit faulty",
                List.of(new CreateSaleReturnRequest.CreateSaleReturnItemRequest(
                        saleItemId, BigDecimal.ONE)),
                List.of(new CreateSaleReturnRequest.CreateSaleReturnRefundRequest(
                        "CASH", unitPrice, null)));

        mockMvc.perform(post("/api/sales/" + saleId + "/returns")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.returnNumber").exists())
                .andExpect(jsonPath("$.data.totalRefundAmount").value(unitPrice.doubleValue()));

        mockMvc.perform(get("/api/sales/" + saleId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_RETURNED"))
                .andExpect(jsonPath("$.data.returnedAmount").value(unitPrice.doubleValue()))
                .andExpect(jsonPath("$.data.items[0].quantityReturned").value(1));

        mockMvc.perform(get("/api/inventory/balances")
                        .param("locationId", String.valueOf(shopWarehouseLocationId))
                        .param("search", "MDL-LED-001")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantityOnHand")
                        .value(qtyBefore.subtract(BigDecimal.ONE).doubleValue()));

        mockMvc.perform(get("/api/inventory/transactions")
                        .param("locationId", String.valueOf(shopWarehouseLocationId))
                        .param("productId", String.valueOf(productId))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.transactionType=='RETURN')]").exists());
    }

    @Test
    void workerCannotProcessReturn() throws Exception {
        long shopId = findShopId(ownerToken, "SHOP-A");
        long productId = findProductId(ownerToken, "MDL-LED-002");
        BigDecimal unitPrice = findProductPrice(ownerToken, "MDL-LED-002");

        CreateSaleRequest saleRequest = new CreateSaleRequest(
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
                        .content(objectMapper.writeValueAsString(saleRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode saleData = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        long saleId = saleData.path("id").asLong();
        long saleItemId = saleData.path("items").get(0).path("id").asLong();

        CreateSaleReturnRequest returnRequest = new CreateSaleReturnRequest(
                "OTHER",
                null,
                List.of(new CreateSaleReturnRequest.CreateSaleReturnItemRequest(
                        saleItemId, BigDecimal.ONE)),
                List.of(new CreateSaleReturnRequest.CreateSaleReturnRefundRequest(
                        "CASH", unitPrice, null)));

        mockMvc.perform(post("/api/sales/" + saleId + "/returns")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isForbidden());
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
