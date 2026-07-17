package com.konverza.scenarios.controller;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.UserRepository;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.job.ScenarioExpirationJob;
import com.konverza.scenarios.repository.ScenarioRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * scenario-privacy-and-lifecycle: Escenario Rápido creator-only privacy
 * (own-vs-all list scoping, 404 on non-owner direct fetch, applied evenly to
 * EMPLOYEE and EXEC), automatic 2-month expiration, and Administrador
 * activate/deactivate for Escenarios Completos. Uses real logins (not
 * @WithMockUser) because ownership is matched against the real
 * authenticated user's id.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScenarioPrivacyTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ScenarioRepository scenarioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ScenarioExpirationJob scenarioExpirationJob;
    @Autowired PlatformTransactionManager transactionManager;
    @PersistenceContext EntityManager entityManager;

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

    private Scenario createQuickScenario(String name, User owner, boolean enabled) {
        return scenarioRepository.save(Scenario.builder()
                .name(name)
                .clientPersona(Scenario.ClientPersona.ANGRY)
                .difficulty(Scenario.Difficulty.EASY)
                .createdBy("EXPRESS_AI")
                .createdByUser(owner)
                .enabled(enabled)
                .build());
    }

    private Scenario createFullScenario(String name, boolean enabled) {
        return scenarioRepository.save(Scenario.builder()
                .name(name)
                .clientPersona(Scenario.ClientPersona.DIFFICULT)
                .difficulty(Scenario.Difficulty.MEDIUM)
                .createdBy("MANUAL")
                .enabled(enabled)
                .build());
    }

    // ── Escenario Rápido privacy ────────────────────────────────────────────

    @Test
    @DisplayName("Creator's list includes their own quick scenario")
    void list_ownQuickScenario_isIncluded() throws Exception {
        String email = "privacy-owner-a@konverza.com";
        String token = seedUserAndLogin(email, User.Role.EMPLOYEE);
        User owner = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Scenario mine = createQuickScenario("Mío A", owner, true);

        MvcResult result = mockMvc.perform(get("/api/scenarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        boolean found = false;
        for (var node : body) if (node.get("id").asText().equals(mine.getId().toString())) found = true;
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("A different EMPLOYEE's list excludes another user's quick scenario")
    void list_otherUsersQuickScenario_isExcluded_forEmployee() throws Exception {
        String ownerEmail = "privacy-owner-b@konverza.com";
        seedUserAndLogin(ownerEmail, User.Role.EMPLOYEE);
        User owner = userRepository.findByEmailIgnoreCase(ownerEmail).orElseThrow();
        Scenario theirs = createQuickScenario("De otro empleado", owner, true);

        String otherToken = seedUserAndLogin("privacy-other-b@konverza.com", User.Role.EMPLOYEE);

        MvcResult result = mockMvc.perform(get("/api/scenarios").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        boolean found = false;
        for (var node : body) if (node.get("id").asText().equals(theirs.getId().toString())) found = true;
        assertThat(found).isFalse();
    }

    @Test
    @DisplayName("EXEC (Autoridad) also cannot see another user's quick scenario in the list")
    void list_otherUsersQuickScenario_isExcluded_forExec() throws Exception {
        String ownerEmail = "privacy-owner-c@konverza.com";
        seedUserAndLogin(ownerEmail, User.Role.EMPLOYEE);
        User owner = userRepository.findByEmailIgnoreCase(ownerEmail).orElseThrow();
        Scenario theirs = createQuickScenario("Escenario privado C", owner, true);

        String execToken = seedUserAndLogin("privacy-exec-c@konverza.com", User.Role.EXEC);

        MvcResult result = mockMvc.perform(get("/api/scenarios").header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        boolean found = false;
        for (var node : body) if (node.get("id").asText().equals(theirs.getId().toString())) found = true;
        assertThat(found).isFalse();
    }

    @Test
    @DisplayName("Requesting another user's quick scenario directly by ID returns 404, not 403")
    void getById_otherUsersQuickScenario_returns404() throws Exception {
        String ownerEmail = "privacy-owner-d@konverza.com";
        seedUserAndLogin(ownerEmail, User.Role.EMPLOYEE);
        User owner = userRepository.findByEmailIgnoreCase(ownerEmail).orElseThrow();
        Scenario theirs = createQuickScenario("Privado D", owner, true);

        String otherToken = seedUserAndLogin("privacy-other-d@konverza.com", User.Role.EMPLOYEE);

        mockMvc.perform(get("/api/scenarios/" + theirs.getId()).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Owner can still fetch their own quick scenario by ID")
    void getById_ownQuickScenario_returns200() throws Exception {
        String email = "privacy-owner-e@konverza.com";
        String token = seedUserAndLogin(email, User.Role.EMPLOYEE);
        User owner = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Scenario mine = createQuickScenario("Mío E", owner, true);

        mockMvc.perform(get("/api/scenarios/" + mine.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── Escenario Completo admin activate/deactivate ────────────────────────

    @Test
    @DisplayName("ADMIN can deactivate a full scenario")
    void setEnabled_asAdmin_disablesFullScenario() throws Exception {
        String adminToken = seedUserAndLogin("privacy-admin-f@konverza.com", User.Role.ADMIN);
        Scenario full = createFullScenario("Completo F", true);

        mockMvc.perform(patch("/api/scenarios/" + full.getId() + "/enabled")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());

        assertThat(scenarioRepository.findById(full.getId()).orElseThrow().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("EMPLOYEE cannot activate/deactivate a full scenario")
    void setEnabled_asEmployee_returns403() throws Exception {
        String employeeToken = seedUserAndLogin("privacy-employee-g@konverza.com", User.Role.EMPLOYEE);
        Scenario full = createFullScenario("Completo G", true);

        mockMvc.perform(patch("/api/scenarios/" + full.getId() + "/enabled")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EXEC cannot activate/deactivate a full scenario")
    void setEnabled_asExec_returns403() throws Exception {
        String execToken = seedUserAndLogin("privacy-exec-h@konverza.com", User.Role.EXEC);
        Scenario full = createFullScenario("Completo H", true);

        mockMvc.perform(patch("/api/scenarios/" + full.getId() + "/enabled")
                        .header("Authorization", "Bearer " + execToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Activating/deactivating a quick scenario is rejected with 400")
    void setEnabled_onQuickScenario_returns400() throws Exception {
        String adminToken = seedUserAndLogin("privacy-admin-i@konverza.com", User.Role.ADMIN);
        Scenario quick = createQuickScenario("Rápido I", userRepository.findByEmailIgnoreCase("privacy-admin-i@konverza.com").orElseThrow(), true);

        mockMvc.perform(patch("/api/scenarios/" + quick.getId() + "/enabled")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A disabled full scenario is excluded from EMPLOYEE's list but still visible to ADMIN")
    void disabledFullScenario_hiddenFromEmployee_visibleToAdmin() throws Exception {
        Scenario disabled = createFullScenario("Completo desactivado J", false);
        String employeeToken = seedUserAndLogin("privacy-employee-j@konverza.com", User.Role.EMPLOYEE);
        String adminToken = seedUserAndLogin("privacy-admin-j@konverza.com", User.Role.ADMIN);

        MvcResult employeeResult = mockMvc.perform(get("/api/scenarios").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();
        var employeeBody = objectMapper.readTree(employeeResult.getResponse().getContentAsString());
        boolean visibleToEmployee = false;
        for (var node : employeeBody) if (node.get("id").asText().equals(disabled.getId().toString())) visibleToEmployee = true;
        assertThat(visibleToEmployee).isFalse();

        mockMvc.perform(get("/api/scenarios/" + disabled.getId()).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound());

        MvcResult adminResult = mockMvc.perform(get("/api/scenarios").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        var adminBody = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        boolean visibleToAdmin = false;
        for (var node : adminBody) if (node.get("id").asText().equals(disabled.getId().toString())) visibleToAdmin = true;
        assertThat(visibleToAdmin).isTrue();
    }

    @Test
    @DisplayName("An enabled full scenario is visible to EMPLOYEE, ADMIN, and EXEC with no per-user assignment")
    void enabledFullScenario_visibleToEveryRole() throws Exception {
        Scenario enabled = createFullScenario("Completo compartido K", true);
        String employeeToken = seedUserAndLogin("privacy-employee-k@konverza.com", User.Role.EMPLOYEE);
        String adminToken = seedUserAndLogin("privacy-admin-k@konverza.com", User.Role.ADMIN);
        String execToken = seedUserAndLogin("privacy-exec-k@konverza.com", User.Role.EXEC);

        for (String token : new String[]{employeeToken, adminToken, execToken}) {
            mockMvc.perform(get("/api/scenarios/" + enabled.getId()).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    // ── Auto-expiration job ─────────────────────────────────────────────────

    @Test
    @DisplayName("Expiration job disables a quick scenario created 2+ months ago")
    void expirationJob_disablesOldQuickScenario() {
        String email = "privacy-expire-owner-l@konverza.com";
        User owner = userRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                userRepository.save(User.builder().email(email).passwordHash(passwordEncoder.encode(PASSWORD))
                        .role(User.Role.EMPLOYEE).enabled(true).build()));
        Scenario old = createQuickScenario("Viejo L", owner, true);
        backdateCreatedAt(old.getId(), LocalDateTime.now().minusMonths(3));

        scenarioExpirationJob.disableExpiredQuickScenarios();

        assertThat(scenarioRepository.findById(old.getId()).orElseThrow().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Expiration job does not disable a quick scenario created less than 2 months ago")
    void expirationJob_leavesRecentQuickScenarioEnabled() {
        String email = "privacy-expire-owner-m@konverza.com";
        User owner = userRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                userRepository.save(User.builder().email(email).passwordHash(passwordEncoder.encode(PASSWORD))
                        .role(User.Role.EMPLOYEE).enabled(true).build()));
        Scenario recent = createQuickScenario("Reciente M", owner, true);

        scenarioExpirationJob.disableExpiredQuickScenarios();

        assertThat(scenarioRepository.findById(recent.getId()).orElseThrow().isEnabled()).isTrue();
    }

    /**
     * @CreationTimestamp fields are updatable=false, so a normal entity
     * save() won't persist a changed createdAt — go straight to SQL to
     * backdate it for the expiration test. Uses an explicit TransactionTemplate
     * (not @Transactional) because a self-invoked annotation wouldn't go
     * through the Spring AOP proxy and would leave no transaction open for
     * the native update.
     */
    void backdateCreatedAt(UUID scenarioId, LocalDateTime createdAt) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.createNativeQuery("UPDATE scenarios SET created_at = :createdAt WHERE id = :id")
                    .setParameter("createdAt", createdAt)
                    .setParameter("id", scenarioId)
                    .executeUpdate();
        });
        entityManager.clear();
    }
}
