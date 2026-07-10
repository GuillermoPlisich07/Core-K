package com.konverza.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /api/dashboard/team — company-wide metrics, ADMIN/EXEC only
 * (add-role-dashboard). Identical for both roles; the read-only distinction
 * lives in the frontend (no write affordances for EXEC), not in this payload.
 */
@Getter @Builder @AllArgsConstructor
public class TeamDashboardResponse {
    private long activeUserCount;
    private List<ActivityPointDTO> activityTrend;
    private BigDecimal avgTeamScore;
    private BigDecimal completionRate;
    private List<CategoryScoreDTO> categoryBreakdown;
    private List<TopPerformerDTO> topPerformers;
}
