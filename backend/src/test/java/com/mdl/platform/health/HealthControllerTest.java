package com.mdl.platform.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private DatabaseHealthService databaseHealthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HealthController controller = new HealthController(databaseHealthService);
        ReflectionTestUtils.setField(controller, "appName", "MDL Platform");
        ReflectionTestUtils.setField(controller, "appVersion", "0.1.0");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void healthEndpointReturnsOkWhenDatabaseIsUp() throws Exception {
        when(databaseHealthService.isDatabaseUp()).thenReturn(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OK"))
                .andExpect(jsonPath("$.data.database").value("UP"));
    }

    @Test
    void healthEndpointReportsDatabaseDown() throws Exception {
        when(databaseHealthService.isDatabaseUp()).thenReturn(false);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.database").value("DOWN"));
    }
}
