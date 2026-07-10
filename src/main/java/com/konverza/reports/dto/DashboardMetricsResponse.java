package com.konverza.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /api/dashboard/me — the caller's own metrics (add-role-dashboard).
 * Empty/zero fields (not an error) when the caller has no completed sessions.
 */
@Getter @Builder @AllArgsConstructor
public class DashboardMetricsResponse {
    private long sessionCount;
    private BigDecimal avgScore;
    private BigDecimal winRate;
    private long practiceTimeSeconds;
    private List<ScorePointDTO> scoreSeries;
    private List<CategoryScoreDTO> categoryBreakdown;
    private List<RecentSessionDTO> recentSessions;
}
