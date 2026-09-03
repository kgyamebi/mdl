package com.mdl.platform.reports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.sales.dto.CreateSaleRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class ExtendedReportsIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_extended_reports_test")
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
    void ownerCanViewExtendedAnalyticsReports() throws Exception {
        mockMvc.perform(get("/api/reports/sales-by-product")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencyCode").value("GHS"))
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/reports/inventory-valuation")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencyCode").value("GHS"))
                .andExpect(jsonPath("$.data.totalCostValue").exists())
                .andExpect(jsonPath("$.data.totalRetailValue").exists())
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/reports/transfer-activity")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTransfers").isNumber())
                .andExpect(jsonPath("$.data.statusCounts").isArray());
    }

    @Test
    void salesByProductReflectsCompletedSale() throws Exception {
        long shopId = findShopId(ownerToken, "SHOP-A");
        long productId = findProductId(ownerToken, "MDL-LED-001");
        BigDecimal unitPrice = findProductPrice(ownerToken, "MDL-LED-001");

        CreateSaleRequest createRequest = new CreateSaleRequest(
                shopId,
                "Report test customer",
                null,
                List.of(new CreateSaleRequest.CreateSaleItemRequest(
                        productId, BigDecimal.valueOf(2), null)),
                List.of(new CreateSaleRequest.CreateSalePaymentRequest(
                        "CASH", unitPrice.multiply(BigDecimal.valueOf(2)), null)));

        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reports/sales-by-product")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("shopId", String.valueOf(shopId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.productSku == 'MDL-LED-001')].quantitySold").value(2.0));
    }

    @Test
    void workerWithoutReportPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/api/reports/sales-by-product")
                        .header("Authorization", "Bearer " + workerToken))
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
