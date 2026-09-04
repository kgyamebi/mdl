package com.mdl.platform.locations;

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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class LocationIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_locations_test")
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
    void ownerSeesAllMainWarehouses() throws Exception {
        mockMvc.perform(get("/api/warehouses?type=MAIN")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].warehouseType").value("MAIN"))
                .andExpect(jsonPath("$.data[0].restricted").value(true));
    }

    @Test
    void ownerSeesBusinessStructure() throws Exception {
        mockMvc.perform(get("/api/business/structure")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.business.code").value("MDL"))
                .andExpect(jsonPath("$.data.business.currencyCode").value("GHS"))
                .andExpect(jsonPath("$.data.mainWarehouses", hasSize(2)))
                .andExpect(jsonPath("$.data.shops", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    void workerSeesOnlyAssignedLocations() throws Exception {
        mockMvc.perform(get("/api/locations")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void workerCannotAccessMainWarehouse() throws Exception {
        MvcResult ownerResult = mockMvc.perform(get("/api/warehouses?type=MAIN")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();

        long mainWarehouseId = objectMapper.readTree(ownerResult.getResponse().getContentAsString())
                .path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/warehouses/" + mainWarehouseId)
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanCreateAndDeactivateShop() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/shops")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Shop East\",\"code\":\"SHOP-TEST-E\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("SHOP-TEST-E"))
                .andExpect(jsonPath("$.data.name").value("Test Shop East"))
                .andReturn();

        long shopId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/api/shops/" + shopId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void ownerCanCreateMainWarehouse() throws Exception {
        mockMvc.perform(post("/api/warehouses")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Regional Hub\",\"code\":\"WH-REG-HUB\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("WH-REG-HUB"))
                .andExpect(jsonPath("$.data.warehouseType").value("MAIN"));
    }

    @Test
    void ownerCanListSupportedCurrencies() throws Exception {
        mockMvc.perform(get("/api/business/currencies")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='GHS')]").exists())
                .andExpect(jsonPath("$.data[?(@.code=='USD')]").exists());
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
