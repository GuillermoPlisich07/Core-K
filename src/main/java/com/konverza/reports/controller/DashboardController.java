package com.konverza.reports.controller;

import com.konverza.reports.dto.DashboardMetricsResponse;
import com.konverza.reports.dto.TeamDashboardResponse;
import com.konverza.reports.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard de bienvenida por rol (add-role-dashboard). /me está disponible
 * para cualquier rol autenticado; /team está restringido a ADMIN/EXEC por la
 * matriz de permisos (add-rbac-permission-matrix — Analytics: Personal | Completo | Completo).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me")
    @Operation(summary = "Métricas personales del usuario autenticado")
    public DashboardMetricsResponse getMyDashboard() {
        return dashboardService.getMyDashboard();
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('ADMIN','EXEC')")
    @Operation(summary = "Métricas de toda la empresa (Admin y Autoridad)")
    public TeamDashboardResponse getTeamDashboard() {
        return dashboardService.getTeamDashboard();
    }
}
