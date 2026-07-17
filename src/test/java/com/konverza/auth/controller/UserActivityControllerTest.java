package com.konverza.auth.controller;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.UserRepository;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.repository.ScenarioRepository;
import com.konverza.sessions.entity.Session;
import com.konverza.sessions.repository.SessionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/users/{id}/activity (user-activity-detail-panel): ADMIN/EXEC-only
 * per-user summary of Escenario Rápido creation + metrics and Escenario
 * Completo completion status — the narrow, explicit exception to
 * scenario-lifecycle's creator-only privacy rule.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserActivityControllerTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ScenarioRepository scenarioRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired PasswordEncoder passwordEncoder;

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

    @Test
    @DisplayName("ADMIN sees a target user's quick-scenario names and metrics")
    void getActivity_asAdmin_showsQuickScenarioMetrics() throws Exception {
        String targetEmail = "activity-target-a@konverza.com";
        seedUserAndLogin(targetEmail, User.Role.EMPLOYEE);
        User target = userRepository.findByEmailIgnoreCase(targetEmail).orElseThrow();

        Scenario quick = scenarioRepository.save(Scenario.builder()
                .name("Escenario propio de A").clientPersona(Scenario.ClientPersona.ANGRY)
                .difficulty(Scenario.Difficulty.EASY).createdBy("EXPRESS_AI").createdByUser(target).build());

        sessionRepository.save(Session.builder().scenario(quick).vendorName("A").user(target)
                .status(Session.Status.COMPLETED).overallScore(new BigDecimal("8.00")).build());
        sessionRepository.save(Session.builder().scenario(quick).vendorName("A").user(target)
                .status(Session.Status.COMPLETED).overallScore(new BigDecimal("6.00")).build());

        String adminToken = seedUserAndLogin("activity-admin-a@konverza.com", User.Role.ADMIN);

        MvcResult result = mockMvc.perform(get("/api/users/" + target.getId() + "/activity")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        var quickScenarios = body.get("quickScenarios");
        boolean found = false;
        for (var node : quickScenarios) {
            if (node.get("id").asText().equals(quick.getId().toString())) {
                found = true;
                assertThat(node.get("name").asText()).isEqualTo("Escenario propio de A");
                assertThat(node.get("sessionCount").asLong()).isEqualTo(2);
                assertThat(node.get("avgScore").asDouble()).isEqualTo(7.00);
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("EXEC (Autoridad) can also fetch the activity endpoint")
    void getActivity_asExec_returns200() throws Exception {
        String targetEmail = "activity-target-b@konverza.com";
        seedUserAndLogin(targetEmail, User.Role.EMPLOYEE);
        User target = userRepository.findByEmailIgnoreCase(targetEmail).orElseThrow();
        String execToken = seedUserAndLogin("activity-exec-b@konverza.com", User.Role.EXEC);

        mockMvc.perform(get("/api/users/" + target.getId() + "/activity")
                        .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EMPLOYEE cannot fetch the activity endpoint")
    void getActivity_asEmployee_returns403() throws Exception {
        String targetEmail = "activity-target-c@konverza.com";
        seedUserAndLogin(targetEmail, User.Role.EMPLOYEE);
        User target = userRepository.findByEmailIgnoreCase(targetEmail).orElseThrow();
        String employeeToken = seedUserAndLogin("activity-employee-c@konverza.com", User.Role.EMPLOYEE);

        mockMvc.perform(get("/api/users/" + target.getId() + "/activity")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Response never includes quick-scenario content fields")
    void getActivity_excludesQuickScenarioContent() throws Exception {
        String targetEmail = "activity-target-d@konverza.com";
        seedUserAndLogin(targetEmail, User.Role.EMPLOYEE);
        User target = userRepository.findByEmailIgnoreCase(targetEmail).orElseThrow();

        scenarioRepository.save(Scenario.builder()
                .name("Escenario privado D").clientPersona(Scenario.ClientPersona.DIFFICULT)
                .difficulty(Scenario.Difficulty.MEDIUM).createdBy("EXPRESS_AI").createdByUser(target)
                .systemPrompt("CONTENIDO SECRETO").objectionsGuide("[]").forbiddenPhrases("[]").faq("[]")
                .build());

        String adminToken = seedUserAndLogin("activity-admin-d@konverza.com", User.Role.ADMIN);

        MvcResult result = mockMvc.perform(get("/api/users/" + target.getId() + "/activity")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("CONTENIDO SECRETO");
        assertThat(body).doesNotContain("systemPrompt");
        assertThat(body).doesNotContain("objectionsGuide");
        assertThat(body).doesNotContain("forbiddenPhrases");
        assertThat(body).doesNotContain("\"faq\"");
    }

    @Test
    @DisplayName("A completed session marks the full scenario completed; only an active/abandoned attempt leaves it pending")
    void getActivity_fullScenarioCompletionStatus() throws Exception {
        String targetEmail = "activity-target-e@konverza.com";
        seedUserAndLogin(targetEmail, User.Role.EMPLOYEE);
        User target = userRepository.findByEmailIgnoreCase(targetEmail).orElseThrow();

        Scenario doneScenario = scenarioRepository.save(Scenario.builder()
                .name("Completo E - hecho").clientPersona(Scenario.ClientPersona.DEMANDING)
                .difficulty(Scenario.Difficulty.HARD).createdBy("MANUAL").enabled(true).build());
        Scenario pendingScenario = scenarioRepository.save(Scenario.builder()
                .name("Completo E - pendiente").clientPersona(Scenario.ClientPersona.DEMANDING)
                .difficulty(Scenario.Difficulty.HARD).createdBy("MANUAL").enabled(true).build());

        sessionRepository.save(Session.builder().scenario(doneScenario).vendorName("E").user(target)
                .status(Session.Status.COMPLETED).build());
        sessionRepository.save(Session.builder().scenario(pendingScenario).vendorName("E").user(target)
                .status(Session.Status.ABANDONED).build());

        String adminToken = seedUserAndLogin("activity-admin-e@konverza.com", User.Role.ADMIN);

        MvcResult result = mockMvc.perform(get("/api/users/" + target.getId() + "/activity")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        boolean doneFound = false, pendingFound = false;
        for (var node : body.get("fullScenarios")) {
            if (node.get("id").asText().equals(doneScenario.getId().toString())) {
                doneFound = true;
                assertThat(node.get("completed").asBoolean()).isTrue();
            }
            if (node.get("id").asText().equals(pendingScenario.getId().toString())) {
                pendingFound = true;
                assertThat(node.get("completed").asBoolean()).isFalse();
            }
        }
        assertThat(doneFound).isTrue();
        assertThat(pendingFound).isTrue();
    }

    @Test
    @DisplayName("Requesting activity for a nonexistent user returns 404")
    void getActivity_nonexistentUser_returns404() throws Exception {
        String adminToken = seedUserAndLogin("activity-admin-f@konverza.com", User.Role.ADMIN);

        mockMvc.perform(get("/api/users/" + java.util.UUID.randomUUID() + "/activity")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
