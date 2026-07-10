package com.konverza.empresa.controller;

import com.konverza.empresa.repository.EmpresaRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Empresa is stricter than Escenarios but matches Productos/Servicios
 * (add-users-empresa-profile): read is ADMIN/EXEC only, write is ADMIN-only,
 * EMPLOYEE (Vendedor) has no access at all, not even read.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmpresaControllerRbacTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EmpresaRepository empresaRepository;

    @AfterEach
    void cleanUp() {
        empresaRepository.deleteAll();
    }

    private String validBody(String name) throws Exception {
        return objectMapper.writeValueAsString(Map.of("name", name, "context", "ctx"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can create and edit the empresa")
    void admin_canCreateAndEdit() throws Exception {
        mockMvc.perform(put("/api/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("Konverza SA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Konverza SA"));

        mockMvc.perform(put("/api/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("Konverza SRL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Konverza SRL"));

        mockMvc.perform(get("/api/empresa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Konverza SRL"));

        assertThat(empresaRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) can read but gets 403 on write")
    void exec_readOnly() throws Exception {
        mockMvc.perform(get("/api/empresa")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("Bloqueado")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE (Vendedor) gets 403 on both read and write")
    void employee_noAccess() throws Exception {
        mockMvc.perform(get("/api/empresa")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("Bloqueado")))
                .andExpect(status().isForbidden());
    }
}
