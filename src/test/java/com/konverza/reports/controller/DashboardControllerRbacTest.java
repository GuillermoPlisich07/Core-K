package com.konverza.reports.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * add-role-dashboard: /api/dashboard/me is open to any authenticated role;
 * /api/dashboard/team is ADMIN/EXEC only (add-rbac-permission-matrix —
 * Analytics: Personal | Completo | Completo).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerRbacTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE can fetch their own dashboard metrics")
    void getMyDashboard_asEmployee_returns200() throws Exception {
        mockMvc.perform(get("/api/dashboard/me")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "EMPLOYEE")
    @DisplayName("EMPLOYEE cannot fetch team-wide dashboard metrics")
    void getTeamDashboard_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/team")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "ADMIN")
    @DisplayName("ADMIN can fetch team-wide dashboard metrics")
    void getTeamDashboard_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/dashboard/team")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "EXEC")
    @DisplayName("EXEC (Autoridad) can fetch team-wide dashboard metrics — read-only, but not blocked")
    void getTeamDashboard_asExec_returns200() throws Exception {
        mockMvc.perform(get("/api/dashboard/team")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "EXEC")
    @DisplayName("EXEC can also fetch their own personal metrics")
    void getMyDashboard_asExec_returns200() throws Exception {
        mockMvc.perform(get("/api/dashboard/me")).andExpect(status().isOk());
    }
}
