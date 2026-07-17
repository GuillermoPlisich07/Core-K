package com.konverza.scenarios.controller;

import com.konverza.scenarios.dto.ScenarioExpressRequest;
import com.konverza.scenarios.dto.ScenarioRequest;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.repository.ScenarioRepository;
import com.konverza.shared.enums.Industry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the add-rbac-permission-matrix rule that scenario writes are
 * ADMIN-only and Autoridad (EXEC) never gets write access anywhere —
 * complements ScenarioControllerTest, which covers the ADMIN-succeeds path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScenarioRbacTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ScenarioRepository scenarioRepository;

    private ScenarioRequest buildRequest(String name) {
        ScenarioRequest req = new ScenarioRequest();
        req.setName(name);
        req.setClientPersona(Scenario.ClientPersona.DIFFICULT);
        req.setDifficulty(Scenario.Difficulty.MEDIUM);
        req.setSystemPrompt("Sos un cliente difícil que evalúa el precio.");
        return req;
    }

    private ScenarioExpressRequest buildExpressRequest() {
        ScenarioExpressRequest req = new ScenarioExpressRequest();
        req.setName("Escenario express de prueba");
        req.setIndustries(java.util.Set.of(Industry.SOFTWARE_B2B));
        req.setClientPersona(Scenario.ClientPersona.DIFFICULT);
        req.setDifficulty(Scenario.Difficulty.MEDIUM);
        req.setProductName("CRM");
        req.setProductDescription("CRM para ventas B2B");
        req.setPriceRange("$500-$1000/mes");
        req.setKeyDifferentiator("Integración nativa con WhatsApp");
        req.setOwnerName("Vendedor de prueba");
        return req;
    }

    private UUID existingScenarioId() {
        return scenarioRepository.findAll().stream().findFirst()
                .map(Scenario::getId)
                .orElseThrow(() -> new IllegalStateException("DataSeeder should have seeded at least one scenario"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE cannot create a scenario")
    void create_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Bloqueado"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot create a scenario")
    void create_asExec_returns403() throws Exception {
        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Bloqueado"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot update a scenario")
    void update_asExec_returns403() throws Exception {
        mockMvc.perform(put("/api/scenarios/" + existingScenarioId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Bloqueado"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE cannot delete a scenario")
    void delete_asEmployee_returns403() throws Exception {
        mockMvc.perform(delete("/api/scenarios/" + existingScenarioId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot delete a scenario")
    void delete_asExec_returns403() throws Exception {
        mockMvc.perform(delete("/api/scenarios/" + existingScenarioId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) can still read the scenario list — read-only, not blocked entirely")
    void list_asExec_returns200() throws Exception {
        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE can still read the scenario list")
    void list_asEmployee_returns200() throws Exception {
        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk());
    }

    /**
     * EMPLOYEE creates escenarios cortos via the Express (AI-generated) flow
     * for their own practice — the one write action Vendedor keeps per the
     * matrix, distinct from the Detallado (largo) flow above, which stays
     * ADMIN-only. EXEC is still never allowed to write.
     */
    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot create an express scenario")
    void createExpress_asExec_returns403() throws Exception {
        // Body must pass @Valid — Spring MVC resolves/validates @RequestBody args
        // before the @PreAuthorize proxy is invoked, so an invalid body would
        // 400 before the role check ever runs.
        mockMvc.perform(post("/api/scenarios/express")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildExpressRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot regenerate a scenario section")
    void regenerateSection_asExec_returns403() throws Exception {
        mockMvc.perform(post("/api/scenarios/" + existingScenarioId() + "/regenerate-section")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"section\":\"systemPrompt\"}"))
                .andExpect(status().isForbidden());
    }

    /**
     * PUT /api/scenarios/{id} is shared by the Detallado edit screen (ADMIN)
     * and the Express review "Guardar" step (EMPLOYEE) — ScenarioService.update
     * enforces per-scenario ownership by origin (createdBy), on top of the
     * role gate on the endpoint itself.
     */
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE can save their own Express-created scenario")
    void update_ownExpressScenario_asEmployee_returns200() throws Exception {
        Scenario expressScenario = scenarioRepository.save(Scenario.builder()
                .name("Borrador express").clientPersona(Scenario.ClientPersona.ANGRY)
                .difficulty(Scenario.Difficulty.EASY).createdBy("EXPRESS_AI").build());

        mockMvc.perform(put("/api/scenarios/" + expressScenario.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Borrador actualizado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Borrador actualizado"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE cannot edit an ADMIN-created (Detallado) scenario, even though PUT is EMPLOYEE-reachable")
    void update_adminCreatedScenario_asEmployee_returns403() throws Exception {
        mockMvc.perform(put("/api/scenarios/" + existingScenarioId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Intento bloqueado"))))
                .andExpect(status().isForbidden());
    }
}
