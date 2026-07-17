package com.konverza.reports.service;

import com.konverza.auth.entity.User;
import com.konverza.auth.security.CurrentUser;
import com.konverza.reports.dto.ActivityPointDTO;
import com.konverza.reports.dto.CategoryScoreDTO;
import com.konverza.reports.dto.DashboardMetricsResponse;
import com.konverza.reports.dto.RecentSessionDTO;
import com.konverza.reports.dto.ScorePointDTO;
import com.konverza.reports.dto.TeamDashboardResponse;
import com.konverza.reports.dto.TopPerformerDTO;
import com.konverza.reports.entity.SessionReport;
import com.konverza.reports.repository.SessionReportRepository;
import com.konverza.sessions.entity.Session;
import com.konverza.sessions.repository.SessionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregates dashboard KPIs from existing Session/SessionReport data
 * (add-role-dashboard). No new tables — plain in-memory aggregation over
 * repository results, acceptable at this app's current scale (see design.md's
 * Risks section); revisit with real SQL aggregation if session volume grows.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal WIN_THRESHOLD = BigDecimal.valueOf(7);
    private static final int SCORE_SERIES_LIMIT = 10;
    private static final int RECENT_SESSIONS_LIMIT = 5;
    private static final int TOP_PERFORMERS_LIMIT = 5;
    private static final long ACTIVE_WINDOW_DAYS = 30;
    private static final int ACTIVITY_TREND_WEEKS = 6;

    private final SessionRepository sessionRepository;
    private final SessionReportRepository sessionReportRepository;

    public DashboardMetricsResponse getMyDashboard() {
        UUID userId = CurrentUser.id();
        List<Session> allSessions = sessionRepository.findAllByUserIdOrderByStartedAtDesc(userId);
        List<Session> completed = completedOnly(allSessions);
        List<SessionReport> reports = sessionReportRepository.findAllBySession_UserId(userId);

        return DashboardMetricsResponse.builder()
                .sessionCount(completed.size())
                .avgScore(averageScore(completed))
                .winRate(winRate(completed))
                .practiceTimeSeconds(totalDuration(completed))
                .scoreSeries(scoreSeries(completed))
                .categoryBreakdown(categoryBreakdown(reports))
                .recentSessions(recentSessions(completed))
                .build();
    }

    public TeamDashboardResponse getTeamDashboard() {
        List<Session> allSessions = sessionRepository.findAllByOrderByStartedAtDesc();
        List<Session> completed = completedOnly(allSessions);
        List<SessionReport> reports = sessionReportRepository.findAll();

        return TeamDashboardResponse.builder()
                .activeUserCount(activeUserCount(allSessions))
                .activityTrend(activityTrend(completed))
                .avgTeamScore(averageScore(completed))
                .completionRate(completionRate(allSessions))
                .categoryBreakdown(categoryBreakdown(reports))
                .topPerformers(topPerformers(completed))
                .build();
    }

    private List<Session> completedOnly(List<Session> sessions) {
        return sessions.stream().filter(s -> s.getStatus() == Session.Status.COMPLETED).toList();
    }

    private BigDecimal averageScore(List<Session> sessions) {
        return ScoreMath.average(sessions.stream().map(Session::getOverallScore).toList());
    }

    private BigDecimal winRate(List<Session> sessions) {
        List<BigDecimal> scores = sessions.stream().map(Session::getOverallScore).filter(java.util.Objects::nonNull).toList();
        if (scores.isEmpty()) return null;
        long wins = scores.stream().filter(s -> s.compareTo(WIN_THRESHOLD) >= 0).count();
        return BigDecimal.valueOf(wins * 100.0 / scores.size()).setScale(1, RoundingMode.HALF_UP);
    }

    private long totalDuration(List<Session> sessions) {
        return sessions.stream().map(Session::getDurationSeconds).filter(java.util.Objects::nonNull)
                .mapToLong(Integer::longValue).sum();
    }

    private List<ScorePointDTO> scoreSeries(List<Session> completedDesc) {
        // completedDesc is newest-first; take the most recent N then reverse to chronological order for charting
        List<Session> recent = completedDesc.stream()
                .filter(s -> s.getOverallScore() != null)
                .limit(SCORE_SERIES_LIMIT)
                .collect(Collectors.toCollection(ArrayList::new));
        java.util.Collections.reverse(recent);
        return recent.stream()
                .map(s -> ScorePointDTO.builder().date(s.getStartedAt()).score(s.getOverallScore()).build())
                .toList();
    }

    private List<RecentSessionDTO> recentSessions(List<Session> completedDesc) {
        return completedDesc.stream().limit(RECENT_SESSIONS_LIMIT)
                .map(s -> RecentSessionDTO.builder()
                        .id(s.getId())
                        .scenarioName(s.getScenario() != null ? s.getScenario().getName() : null)
                        .score(s.getOverallScore())
                        .startedAt(s.getStartedAt())
                        .durationSeconds(s.getDurationSeconds())
                        .build())
                .toList();
    }

    private List<CategoryScoreDTO> categoryBreakdown(List<SessionReport> reports) {
        return List.of(
                categoryAvg("Persuasion", reports, SessionReport::getScorePersuasion),
                categoryAvg("Confianza", reports, SessionReport::getScoreConfidence),
                categoryAvg("Conocimiento del producto", reports, SessionReport::getScoreProductKnowledge),
                categoryAvg("Manejo de objeciones", reports, SessionReport::getScoreObjectionHandling),
                categoryAvg("Pronunciación", reports, SessionReport::getScorePronunciation)
        );
    }

    private CategoryScoreDTO categoryAvg(String label, List<SessionReport> reports,
                                          java.util.function.Function<SessionReport, BigDecimal> extractor) {
        List<BigDecimal> values = reports.stream().map(extractor).filter(java.util.Objects::nonNull).toList();
        BigDecimal avg = values.isEmpty() ? null : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        return CategoryScoreDTO.builder().category(label).avgScore(avg).build();
    }

    private long activeUserCount(List<Session> allSessions) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(ACTIVE_WINDOW_DAYS);
        return allSessions.stream()
                .filter(s -> s.getUser() != null && s.getStartedAt() != null && s.getStartedAt().isAfter(cutoff))
                .map(s -> s.getUser().getId())
                .distinct()
                .count();
    }

    private BigDecimal completionRate(List<Session> allSessions) {
        if (allSessions.isEmpty()) return null;
        long completedCount = allSessions.stream().filter(s -> s.getStatus() == Session.Status.COMPLETED).count();
        return BigDecimal.valueOf(completedCount * 100.0 / allSessions.size()).setScale(1, RoundingMode.HALF_UP);
    }

    private List<ActivityPointDTO> activityTrend(List<Session> completed) {
        LocalDateTime cutoff = LocalDateTime.now().minusWeeks(ACTIVITY_TREND_WEEKS);
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        Map<Integer, Long> byWeek = completed.stream()
                .filter(s -> s.getStartedAt() != null && s.getStartedAt().isAfter(cutoff))
                .collect(Collectors.groupingBy(s -> s.getStartedAt().get(weekFields.weekOfWeekBasedYear()), Collectors.counting()));
        return byWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> ActivityPointDTO.builder().label("W" + e.getKey()).sessionCount(e.getValue()).build())
                .toList();
    }

    private List<TopPerformerDTO> topPerformers(List<Session> completed) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(ACTIVE_WINDOW_DAYS);
        // Group by user id (not the User entity itself) to avoid relying on
        // JPA first-level-cache identity for equality across grouped rows.
        Map<UUID, List<Session>> byUserId = completed.stream()
                .filter(s -> s.getUser() != null && s.getStartedAt() != null && s.getStartedAt().isAfter(cutoff))
                .collect(Collectors.groupingBy(s -> s.getUser().getId()));

        return byUserId.values().stream()
                .map(userSessions -> {
                    User user = userSessions.get(0).getUser();
                    return TopPerformerDTO.builder()
                            .email(user.getEmail())
                            .avgScore(averageScore(userSessions))
                            .sessionCount(userSessions.size())
                            .build();
                })
                .filter(p -> p.getAvgScore() != null)
                .sorted(Comparator.comparing(TopPerformerDTO::getAvgScore).reversed())
                .limit(TOP_PERFORMERS_LIMIT)
                .toList();
    }
}
