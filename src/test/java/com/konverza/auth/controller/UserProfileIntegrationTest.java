package com.konverza.auth.controller;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the first-login profile-completion gate end to end
 * (add-users-empresa-profile): the session flag toggling from false to true
 * once the self-service profile endpoint is called, and the ownership scoping
 * of that endpoint (a user can only ever touch their own row).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User seedUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(User.Role.EMPLOYEE)
                .enabled(true)
                .build());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String profileBody(int age, String personality, String selfDescription) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "age", age, "personality", personality, "selfDescription", selfDescription));
    }

    @Test
    @DisplayName("login response has profileCompleted:false until the profile is submitted, then true")
    void profileCompleted_flipsAfterSubmission() throws Exception {
        String email = "incompleto@konverza.com";
        seedUser(email);

        MvcResult firstLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleted").value(false))
                .andReturn();
        String token = objectMapper.readTree(firstLogin.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody(30, "Analitico", "Vendedor con 5 anos de experiencia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleted").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleted").value(true));
    }

    @Test
    @DisplayName("GET /api/users/me returns the caller's own row")
    void getOwnProfile_returnsCallerRow() throws Exception {
        String email = "propio@konverza.com";
        User user = seedUser(email);
        String token = login(email);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("PUT /api/users/me/profile only ever updates the caller's own row")
    void updateOwnProfile_neverTouchesAnotherUser() throws Exception {
        String callerEmail = "caller@konverza.com";
        String otherEmail = "other@konverza.com";
        seedUser(callerEmail);
        User other = seedUser(otherEmail);
        String token = login(callerEmail);

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody(25, "Extrovertido", "Nuevo en el equipo")))
                .andExpect(status().isOk());

        User otherAfter = userRepository.findById(other.getId()).orElseThrow();
        assertThat(otherAfter.isProfileCompleted()).isFalse();
        assertThat(otherAfter.getAge()).isNull();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("PUT /api/users/me/profile rejects an incomplete body")
    void updateOwnProfile_missingFields_returns400() throws Exception {
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
