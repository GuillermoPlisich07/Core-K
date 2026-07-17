package com.konverza.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** GET /api/users/{id}/activity — ADMIN/EXEC only (user-activity-detail-panel). */
@Getter @Builder @AllArgsConstructor
public class UserActivityResponse {
    private List<QuickScenarioActivityDTO> quickScenarios;
    private List<FullScenarioActivityDTO> fullScenarios;
}
