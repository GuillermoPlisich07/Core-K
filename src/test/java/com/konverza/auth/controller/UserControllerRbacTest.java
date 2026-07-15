package com.konverza.auth.controller;

import com.konverza.auth.dto.CreateUserRequest;
import com.konverza.auth.dto.UpdateUserRequest;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * User management is the clearest ADMIN-only power in the permission matrix
 * (add-rbac-permission-matrix): Vendedor has no access at all, Autoridad can
 * list (read-only) but never write, and only Administrador can create, edit,
 * or delete accounts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerRbacTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private CreateUserRequest buildCreateRequest(String email) {
        CreateUserRequest req = new CreateUserRequest();
        req.setFirstName("Test");
        req.setLastName("User");
        req.setEmail(email);
        req.setPassword("Some-Password-1!");
        req.setRole(User.Role.EMPLOYEE);
        return req;
    }

    private UUID seedThrowawayUser(String email) {
        User saved = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("whatever"))
                .role(User.Role.EMPLOYEE)
                .enabled(true)
                .build());
        return saved.getId();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE cannot list users")
    void list_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) can list users — read-only")
    void list_asExec_returns200() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can list users")
    void list_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE cannot create a user")
    void create_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("bloqueado1@konverza.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot create a user")
    void create_asExec_returns403() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("bloqueado2@konverza.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can create a user")
    void create_asAdmin_returns201() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("nuevo-rbac-test@konverza.com"))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can create a user with role EXEC (Autoridad) — the only UI-reachable way to create that role")
    void create_execRole_asAdmin_returns201() throws Exception {
        CreateUserRequest req = buildCreateRequest("nueva-autoridad@konverza.com");
        req.setRole(User.Role.EXEC);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EXEC"));
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot change a user's role")
    void update_asExec_returns403() throws Exception {
        UUID id = seedThrowawayUser("exec-cant-touch@konverza.com");
        UpdateUserRequest req = new UpdateUserRequest();
        req.setRole(User.Role.ADMIN);
        req.setEnabled(true);

        mockMvc.perform(put("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE cannot delete a user")
    void delete_asEmployee_returns403() throws Exception {
        UUID id = seedThrowawayUser("employee-cant-delete@konverza.com");
        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot delete a user")
    void delete_asExec_returns403() throws Exception {
        UUID id = seedThrowawayUser("exec-cant-delete@konverza.com");
        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can delete a user")
    void delete_asAdmin_returns204() throws Exception {
        UUID id = seedThrowawayUser("admin-can-delete@konverza.com");
        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isNoContent());
    }
}
