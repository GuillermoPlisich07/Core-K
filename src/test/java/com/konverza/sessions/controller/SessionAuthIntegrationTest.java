package com.konverza.sessions.controller;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.UserRepository;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.repository.ScenarioRepository;
import com.konverza.shared.seed.DataSeeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the add-service-auth requirements that span Core-k's user-facing
 * auth (add-user-login) and the delegated/service credentials this change
 * adds: POST /api/sessions mints a delegated token for the authenticated
 * user, and GET /api/scenarios/{id} accepts AI-Service-k's service key as an
 * alternative to a user Bearer token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionAuthIntegrationTest {

    private static final String EMAIL = "vendedor.session-auth@konverza.com";
    private static final String PASSWORD = "correct-horse-battery";
    private static final String SERVICE_JWT_SECRET =
            "test-only-service-jwt-signing-secret-not-for-any-real-use";
    private static final String AI_SERVICE_API_KEY = "test-only-ai-service-api-key";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ScenarioRepository scenarioRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID scenarioId;

    @BeforeEach
    void seedUserAndScenario() {
        userRepository.findByEmailIgnoreCase(EMAIL).ifPresentOrElse(
                existing -> {},
                () -> userRepository.save(User.builder()
                        .email(EMAIL)
                        .passwordHash(passwordEncoder.encode(PASSWORD))
                        .role(User.Role.EMPLOYEE)
                        .enabled(true)
                        .build())
        );
        scenarioId = scenarioRepository.findAll().stream().findFirst()
                .map(Scenario::getId)
                .orElseThrow(() -> new IllegalStateException("DataSeeder should have seeded at least one scenario"));
    }

    private String loginAndGetAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private Claims decodeDelegatedToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SERVICE_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).requireIssuer("core-k").build()
                .parseSignedClaims(token).getPayload();
    }

    @Test
    @DisplayName("POST /api/sessions mints a delegated token scoped to the created session")
    void createSession_returnsDelegatedTokenWithExpectedClaims() throws Exception {
        String accessToken = loginAndGetAccessToken();

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scenarioId", scenarioId.toString(),
                                "vendorName", "Juan"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delegatedToken").isNotEmpty())
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        String sessionId = body.get("id").asText();
        String delegatedToken = body.get("delegatedToken").asText();

        Claims claims = decodeDelegatedToken(delegatedToken);
        assertThat(claims.get("session_id", String.class)).isEqualTo(sessionId);
        assertThat(claims.get("scenario_id", String.class)).isEqualTo(scenarioId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("EMPLOYEE");
        assertThat(claims.getAudience()).contains("ai-service-k");
    }

    @Test
    @DisplayName("POST /api/sessions without a user token returns 401")
    void createSession_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scenarioId", scenarioId.toString(),
                                "vendorName", "Juan"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/scenarios/{id} accepts a valid X-Service-Key without any user token")
    void getScenario_withValidServiceKey_returns200() throws Exception {
        mockMvc.perform(get("/api/scenarios/" + scenarioId)
                        .header("X-Service-Key", AI_SERVICE_API_KEY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/scenarios/{id} rejects an invalid X-Service-Key with no user token")
    void getScenario_withInvalidServiceKey_returns401() throws Exception {
        mockMvc.perform(get("/api/scenarios/" + scenarioId)
                        .header("X-Service-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }
}
