package com.konverza.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konverza.dto.ScenarioRequest;
import com.konverza.entity.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScenarioControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private ScenarioRequest buildRequest(String name) {
        ScenarioRequest req = new ScenarioRequest();
        req.setName(name);
        req.setClientPersona(Scenario.ClientPersona.DIFFICULT);
        req.setDifficulty(Scenario.Difficulty.MEDIUM);
        req.setProductContext("Software CRM para ventas B2B");
        req.setSystemPrompt("Sos un cliente difícil que evalúa el precio.");
        return req;
    }

    @Test
    @DisplayName("GET /api/scenarios returns 200 with JSON list")
    void listScenarios_returnsOk() throws Exception {
        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/scenarios creates scenario and returns 201")
    void createScenario_returns201() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest("Escenario de prueba"));

        MvcResult result = mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Scenario created = objectMapper.readValue(
                result.getResponse().getContentAsString(), Scenario.class);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Escenario de prueba");
    }

    @Test
    @DisplayName("GET /api/scenarios/{id} returns scenario by id")
    void getById_existingScenario_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest("Para buscar"));
        MvcResult created = mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/api/scenarios/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Para buscar"));
    }

    @Test
    @DisplayName("PUT /api/scenarios/{id} updates the scenario name")
    void updateScenario_updatesName() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest("Original"));
        MvcResult created = mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(put("/api/scenarios/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Actualizado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Actualizado"));
    }

    @Test
    @DisplayName("DELETE /api/scenarios/{id} returns 204")
    void deleteScenario_returns204() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest("Para borrar"));
        MvcResult created = mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/api/scenarios/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/scenarios without required fields returns 400")
    void createScenario_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
