package com.konverza.sessions.controller;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.UserRepository;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.repository.ScenarioRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Own-vs-all session scoping and the EXEC-cannot-run-simulations rule
 * (add-rbac-permission-matrix). Uses real logins (not @WithMockUser) because
 * SessionService resolves the caller's User row from the JWT subject —
 * @WithMockUser's default principal name isn't a valid UUID.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionRbacTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ScenarioRepository scenarioRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID scenarioId;

    @BeforeEach
    void setUp() {
        scenarioId = scenarioRepository.findAll().stream().findFirst()
                .map(Scenario::getId)
                .orElseThrow(() -> new IllegalStateException("DataSeeder should have seeded at least one scenario"));
    }

    private String seedUserAndLogin(String email, User.Role role) throws Exception {
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
                existing -> {},
                () -> userRepository.save(User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode(PASSWORD))
                        .role(role)
                        .enabled(true)
                        .build())
        );
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createSession(String accessToken, String vendorName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scenarioId", scenarioId.toString(),
                                "vendorName", vendorName
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    @DisplayName("EXEC (Autoridad) cannot start a live simulation")
    void createSession_asExec_returns403() throws Exception {
        String execToken = seedUserAndLogin("rbac-exec@konverza.com", User.Role.EXEC);

        mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + execToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scenarioId", scenarioId.toString(),
                                "vendorName", "Autoridad"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EXEC (Autoridad) cannot complete a session")
    void completeSession_asExec_returns403() throws Exception {
        String employeeToken = seedUserAndLogin("rbac-owner-for-complete@konverza.com", User.Role.EMPLOYEE);
        String sessionId = createSession(employeeToken, "Vendedor");
        String execToken = seedUserAndLogin("rbac-exec-complete@konverza.com", User.Role.EXEC);

        mockMvc.perform(put("/api/sessions/" + sessionId + "/complete")
                        .header("Authorization", "Bearer " + execToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "durationSeconds", 60, "totalTurns", 2, "transcript", java.util.List.of()
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMPLOYEE's session list only contains their own sessions")
    void listSessions_asEmployee_onlyOwnSessions() throws Exception {
        String tokenA = seedUserAndLogin("rbac-vendor-a@konverza.com", User.Role.EMPLOYEE);
        String tokenB = seedUserAndLogin("rbac-vendor-b@konverza.com", User.Role.EMPLOYEE);
        String sessionA = createSession(tokenA, "Vendedor A");
        createSession(tokenB, "Vendedor B");

        MvcResult result = mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        var ids = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(ids.isArray()).isTrue();
        for (var node : ids) {
            assertThat(node.get("id").asText()).isEqualTo(sessionA);
        }
    }

    @Test
    @DisplayName("ADMIN's session list contains sessions from every user")
    void listSessions_asAdmin_containsAllUsersSessions() throws Exception {
        String tokenA = seedUserAndLogin("rbac-vendor-c@konverza.com", User.Role.EMPLOYEE);
        String sessionC = createSession(tokenA, "Vendedor C");
        String adminToken = seedUserAndLogin("rbac-admin-list@konverza.com", User.Role.ADMIN);

        MvcResult result = mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        var ids = objectMapper.readTree(result.getResponse().getContentAsString());
        boolean containsSessionC = false;
        for (var node : ids) {
            if (node.get("id").asText().equals(sessionC)) containsSessionC = true;
        }
        assertThat(containsSessionC).isTrue();
    }

    @Test
    @DisplayName("EMPLOYEE requesting another vendor's session by id gets 404, not 403")
    void getById_otherVendorsSession_returns404() throws Exception {
        String tokenA = seedUserAndLogin("rbac-vendor-d@konverza.com", User.Role.EMPLOYEE);
        String sessionD = createSession(tokenA, "Vendedor D");
        String tokenE = seedUserAndLogin("rbac-vendor-e@konverza.com", User.Role.EMPLOYEE);

        mockMvc.perform(get("/api/sessions/" + sessionD)
                        .header("Authorization", "Bearer " + tokenE))
                .andExpect(status().isNotFound());
    }
}
