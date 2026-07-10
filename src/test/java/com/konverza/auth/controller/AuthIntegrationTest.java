package com.konverza.auth.controller;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.RefreshTokenRepository;
import com.konverza.auth.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String EMAIL = "vendedor.integration@konverza.com";
    private static final String PASSWORD = "correct-horse-battery";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        userRepository.findByEmailIgnoreCase(EMAIL).ifPresentOrElse(
                existing -> {},
                () -> userRepository.save(User.builder()
                        .email(EMAIL)
                        .passwordHash(passwordEncoder.encode(PASSWORD))
                        .role(User.Role.EMPLOYEE)
                        .enabled(true)
                        .build())
        );
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    @Test
    @DisplayName("GET /api/scenarios without a token returns 401")
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/scenarios with a valid access token returns 200")
    void protectedEndpoint_validToken_returns200() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/scenarios").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/scenarios with a malformed token returns 401")
    void protectedEndpoint_malformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/scenarios").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login with valid credentials returns access token, role, and a refresh cookie")
    void login_validCredentials_returnsSessionAndCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("employee"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("login with an invalid password returns 401 with INVALID_CREDENTIALS code")
    void login_invalidPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("login for a disabled account returns 403 with ACCOUNT_DISABLED code")
    void login_disabledAccount_returns403() throws Exception {
        String disabledEmail = "disabled.integration@konverza.com";
        userRepository.save(User.builder()
                .email(disabledEmail)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(User.Role.EMPLOYEE)
                .enabled(false)
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(disabledEmail, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    @DisplayName("refresh with the cookie from login returns a new access token")
    void refresh_validCookie_returnsNewAccessToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("refresh without a cookie returns 401")
    void refresh_noCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout revokes the refresh token so it can no longer be used")
    void logout_thenRefresh_returns401() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }
}
