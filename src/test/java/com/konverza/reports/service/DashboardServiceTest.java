package com.konverza.reports.service;

import com.konverza.auth.entity.User;
import com.konverza.reports.dto.DashboardMetricsResponse;
import com.konverza.reports.dto.TeamDashboardResponse;
import com.konverza.reports.entity.SessionReport;
import com.konverza.reports.repository.SessionReportRepository;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.sessions.entity.Session;
import com.konverza.sessions.repository.SessionRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private SessionReportRepository sessionReportRepository;

    private DashboardService dashboardService;
    private UUID callerId;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(sessionRepository, sessionReportRepository);
        callerId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(callerId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User user(String email) {
        return User.builder().id(UUID.randomUUID()).email(email).role(User.Role.EMPLOYEE).build();
    }

    private Scenario scenario(String name) {
        return Scenario.builder().id(UUID.randomUUID()).name(name)
                .clientPersona(Scenario.ClientPersona.ANGRY).difficulty(Scenario.Difficulty.MEDIUM).build();
    }

    private Session completedSession(User owner, BigDecimal score, int durationSeconds, LocalDateTime startedAt) {
        return Session.builder().id(UUID.randomUUID()).user(owner).scenario(scenario("Escenario"))
                .vendorName(owner.getEmail()).status(Session.Status.COMPLETED)
                .overallScore(score).durationSeconds(durationSeconds).startedAt(startedAt).build();
    }

    @Test
    @DisplayName("getMyDashboard returns zeroed metrics, not an error, when the caller has no sessions")
    void getMyDashboard_noSessions_returnsEmptyMetrics() {
        when(sessionRepository.findAllByUserIdOrderByStartedAtDesc(callerId)).thenReturn(List.of());
        when(sessionReportRepository.findAllBySession_UserId(callerId)).thenReturn(List.of());

        DashboardMetricsResponse result = dashboardService.getMyDashboard();

        assertThat(result.getSessionCount()).isEqualTo(0);
        assertThat(result.getAvgScore()).isNull();
        assertThat(result.getWinRate()).isNull();
        assertThat(result.getPracticeTimeSeconds()).isEqualTo(0);
        assertThat(result.getScoreSeries()).isEmpty();
        assertThat(result.getRecentSessions()).isEmpty();
        assertThat(result.getCategoryBreakdown()).hasSize(5);
        assertThat(result.getCategoryBreakdown()).allSatisfy(c -> assertThat(c.getAvgScore()).isNull());
    }

    @Test
    @DisplayName("getMyDashboard computes session count, avg score, win rate and practice time from completed sessions only")
    void getMyDashboard_mixedSessions_computesCorrectAggregates() {
        User me = user("vendedor@konverza.com");
        Session won = completedSession(me, BigDecimal.valueOf(8.5), 300, LocalDateTime.now().minusDays(1));
        Session lost = completedSession(me, BigDecimal.valueOf(5.0), 200, LocalDateTime.now().minusDays(2));
        Session active = Session.builder().id(UUID.randomUUID()).user(me).scenario(scenario("En curso"))
                .vendorName("x").status(Session.Status.ACTIVE).startedAt(LocalDateTime.now()).build();

        when(sessionRepository.findAllByUserIdOrderByStartedAtDesc(callerId)).thenReturn(List.of(won, lost, active));
        when(sessionReportRepository.findAllBySession_UserId(callerId)).thenReturn(List.of());

        DashboardMetricsResponse result = dashboardService.getMyDashboard();

        assertThat(result.getSessionCount()).isEqualTo(2); // active session excluded
        assertThat(result.getAvgScore()).isEqualByComparingTo("6.75");
        assertThat(result.getWinRate()).isEqualByComparingTo("50.0"); // 1 of 2 >= 7
        assertThat(result.getPracticeTimeSeconds()).isEqualTo(500);
        assertThat(result.getRecentSessions()).hasSize(2);
    }

    @Test
    @DisplayName("getMyDashboard averages SessionReport category scores, ignoring nulls")
    void getMyDashboard_categoryBreakdown_averagesNonNullScores() {
        SessionReport r1 = SessionReport.builder().id(UUID.randomUUID())
                .scorePersuasion(BigDecimal.valueOf(8)).scoreConfidence(BigDecimal.valueOf(6)).build();
        SessionReport r2 = SessionReport.builder().id(UUID.randomUUID())
                .scorePersuasion(BigDecimal.valueOf(6)).build(); // scoreConfidence null — must not skew the average to 0

        when(sessionRepository.findAllByUserIdOrderByStartedAtDesc(callerId)).thenReturn(List.of());
        when(sessionReportRepository.findAllBySession_UserId(callerId)).thenReturn(List.of(r1, r2));

        var breakdown = dashboardService.getMyDashboard().getCategoryBreakdown();

        var persuasion = breakdown.stream().filter(c -> c.getCategory().equals("Persuasion")).findFirst().orElseThrow();
        var confidence = breakdown.stream().filter(c -> c.getCategory().equals("Confianza")).findFirst().orElseThrow();
        assertThat(persuasion.getAvgScore()).isEqualByComparingTo("7.00");
        assertThat(confidence.getAvgScore()).isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("getTeamDashboard returns zeroed metrics, not an error, when there is no company activity")
    void getTeamDashboard_noActivity_returnsEmptyMetrics() {
        when(sessionRepository.findAllByOrderByStartedAtDesc()).thenReturn(List.of());
        when(sessionReportRepository.findAll()).thenReturn(List.of());

        TeamDashboardResponse result = dashboardService.getTeamDashboard();

        assertThat(result.getActiveUserCount()).isEqualTo(0);
        assertThat(result.getAvgTeamScore()).isNull();
        assertThat(result.getCompletionRate()).isNull();
        assertThat(result.getActivityTrend()).isEmpty();
        assertThat(result.getTopPerformers()).isEmpty();
    }

    @Test
    @DisplayName("getTeamDashboard aggregates across all users and computes completion rate over all statuses")
    void getTeamDashboard_multipleUsers_aggregatesAcrossCompany() {
        User alice = user("alice@konverza.com");
        User bob = user("bob@konverza.com");
        Session aliceWin = completedSession(alice, BigDecimal.valueOf(9.0), 100, LocalDateTime.now().minusDays(1));
        Session bobLoss = completedSession(bob, BigDecimal.valueOf(4.0), 100, LocalDateTime.now().minusDays(1));
        Session abandoned = Session.builder().id(UUID.randomUUID()).user(alice).scenario(scenario("x"))
                .vendorName("x").status(Session.Status.ABANDONED).startedAt(LocalDateTime.now()).build();

        when(sessionRepository.findAllByOrderByStartedAtDesc()).thenReturn(List.of(aliceWin, bobLoss, abandoned));
        when(sessionReportRepository.findAll()).thenReturn(List.of());

        TeamDashboardResponse result = dashboardService.getTeamDashboard();

        assertThat(result.getActiveUserCount()).isEqualTo(2); // alice + bob, distinct
        assertThat(result.getCompletionRate()).isEqualByComparingTo("66.7"); // 2 completed of 3 total
        assertThat(result.getTopPerformers()).hasSize(2);
        assertThat(result.getTopPerformers().get(0).getEmail()).isEqualTo("alice@konverza.com"); // highest score first
    }
}
