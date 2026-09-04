package com.mdl.platform.reports;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class ReportExportIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_report_export_test")
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
    void ownerCanExportSalesSummaryCsv() throws Exception {
        mockMvc.perform(get("/api/reports/sales-summary/export")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("sales-summary-MDL")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("net_sales_amount")));

        mockMvc.perform(get("/api/reports/exports")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].reportType").value("SALES_SUMMARY"));
    }

    @Test
    void ownerCanExportInventoryBalancesCsv() throws Exception {
        mockMvc.perform(get("/api/reports/inventory-balances/export")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("inventory-balances-MDL")))
                .andExpect(content().string(containsString("quantity_on_hand")));
    }

    @Test
    void ownerCanExportSalesSummaryPdf() throws Exception {
        mockMvc.perform(get("/api/reports/sales-summary/export/pdf")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("sales-summary-MDL")))
                .andExpect(header().string("Content-Type", containsString("application/pdf")));

        mockMvc.perform(get("/api/reports/exports")
                        .param("reportType", "SALES_SUMMARY")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].exportFormat").value("PDF"));
    }

    @Test
    void workerCannotExportReports() throws Exception {
        mockMvc.perform(get("/api/reports/sales-summary/export")
                        .header("Authorization", "Bearer " + workerToken))
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
