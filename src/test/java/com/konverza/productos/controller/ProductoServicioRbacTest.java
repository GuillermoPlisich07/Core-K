package com.konverza.productos.controller;

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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers both /api/productos and /api/servicios — identical role rules
 * (add-rbac-permission-matrix): read is ADMIN/EXEC only (EMPLOYEE has no
 * access at all, unlike Scenarios), write is ADMIN-only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoServicioRbacTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String validBody(String name) throws Exception {
        return objectMapper.writeValueAsString(Map.of("name", name, "description", "desc", "context", "ctx"));
    }

    // --- ADMIN: full CRUD ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can create, read, update, and delete a producto")
    void admin_fullCrud_producto() throws Exception {
        assertFullCrud("/api/productos");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can create, read, update, and delete a servicio")
    void admin_fullCrud_servicio() throws Exception {
        assertFullCrud("/api/servicios");
    }

    private void assertFullCrud(String basePath) throws Exception {
        MvcResult created = mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("Original")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Original"))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get(basePath + "/" + id)).andExpect(status().isOk());

        mockMvc.perform(put(basePath + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("Actualizado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Actualizado"));

        mockMvc.perform(delete(basePath + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(basePath + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN creating a producto/servicio without a name gets 400")
    void admin_missingName_returns400() throws Exception {
        for (String path : new String[]{"/api/productos", "/api/servicios"}) {
            mockMvc.perform(post(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // --- EXEC (Autoridad): read-only ---

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) can list productos and servicios")
    void exec_canList() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/servicios")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot create a producto or servicio")
    void exec_cannotCreate() throws Exception {
        for (String path : new String[]{"/api/productos", "/api/servicios"}) {
            mockMvc.perform(post(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody("Bloqueado")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(roles = "EXEC")
    @DisplayName("EXEC (Autoridad) cannot update or delete a producto or servicio")
    void exec_cannotUpdateOrDelete() throws Exception {
        UUID randomId = UUID.randomUUID();
        for (String path : new String[]{"/api/productos", "/api/servicios"}) {
            mockMvc.perform(put(path + "/" + randomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody("Bloqueado")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(path + "/" + randomId))
                    .andExpect(status().isForbidden());
        }
    }

    // --- EMPLOYEE (Vendedor): no access at all, not even read ---

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE (Vendedor) cannot even list productos or servicios")
    void employee_cannotList() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/servicios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE (Vendedor) cannot create a producto or servicio")
    void employee_cannotCreate() throws Exception {
        for (String path : new String[]{"/api/productos", "/api/servicios"}) {
            mockMvc.perform(post(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody("Bloqueado")))
                    .andExpect(status().isForbidden());
        }
    }
}
