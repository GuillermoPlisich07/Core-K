package com.konverza.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * fix-core-k-healthcheck: /actuator/health must be reachable without any
 * credential — it's what docker-compose's healthcheck relies on, since
 * every other route requires either a user or service credential.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorHealthTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("GET /actuator/health returns 200 UP without any Authorization header")
    void health_noToken_returnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
