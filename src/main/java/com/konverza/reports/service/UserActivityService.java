package com.konverza.reports.service;

import com.konverza.auth.exception.UserNotFoundException;
import com.konverza.auth.repository.UserRepository;
import com.konverza.reports.dto.FullScenarioActivityDTO;
import com.konverza.reports.dto.QuickScenarioActivityDTO;
import com.konverza.reports.dto.UserActivityResponse;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.repository.ScenarioRepository;
import com.konverza.sessions.entity.Session;
import com.konverza.sessions.repository.SessionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Per-user activity summary backing GET /api/users/{id}/activity
 * (user-activity-detail-panel), ADMIN/EXEC only. Deliberately bypasses
 * ScenarioService.isVisibleToCurrentUser's creator-only scoping for
 * Escenarios Rápidos — the narrow exception documented in
 * scenario-lifecycle's "Escenario Rápido is private to its creator"
 * requirement. Only name + aggregate metrics are exposed here, never a
 * scenario's content or any session transcript.
 */
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private static final String EXPRESS_ORIGIN = "EXPRESS_AI";
    private static final String MANUAL_ORIGIN = "MANUAL";

    private final UserRepository userRepository;
    private final ScenarioRepository scenarioRepository;
    private final SessionRepository sessionRepository;

    public UserActivityResponse getUserActivity(UUID targetUserId) {
        userRepository.findById(targetUserId).orElseThrow(() -> new UserNotFoundException(targetUserId));

        List<Session> userSessions = sessionRepository.findAllByUserIdOrderByStartedAtDesc(targetUserId);
        Map<UUID, List<Session>> sessionsByScenarioId = userSessions.stream()
                .filter(s -> s.getScenario() != null)
                .collect(Collectors.groupingBy(s -> s.getScenario().getId()));

        return UserActivityResponse.builder()
                .quickScenarios(quickScenarioActivity(targetUserId, sessionsByScenarioId))
                .fullScenarios(fullScenarioActivity(sessionsByScenarioId))
                .build();
    }

    private List<QuickScenarioActivityDTO> quickScenarioActivity(UUID targetUserId, Map<UUID, List<Session>> sessionsByScenarioId) {
        List<Scenario> quickScenarios = scenarioRepository.findByCreatedByUser_IdAndCreatedBy(targetUserId, EXPRESS_ORIGIN);
        return quickScenarios.stream()
                .map(scenario -> {
                    List<Session> sessions = sessionsByScenarioId.getOrDefault(scenario.getId(), List.of());
                    return QuickScenarioActivityDTO.builder()
                            .id(scenario.getId())
                            .name(scenario.getName())
                            .createdAt(scenario.getCreatedAt())
                            .enabled(scenario.isEnabled())
                            .sessionCount(sessions.size())
                            .avgScore(ScoreMath.average(sessions.stream().map(Session::getOverallScore).toList()))
                            .build();
                })
                .toList();
    }

    private List<FullScenarioActivityDTO> fullScenarioActivity(Map<UUID, List<Session>> sessionsByScenarioId) {
        List<Scenario> fullScenarios = scenarioRepository.findByCreatedByAndEnabledTrue(MANUAL_ORIGIN);
        return fullScenarios.stream()
                .map(scenario -> {
                    List<Session> completed = sessionsByScenarioId.getOrDefault(scenario.getId(), List.of()).stream()
                            .filter(s -> s.getStatus() == Session.Status.COMPLETED)
                            .toList();
                    LocalDateTime lastCompletedAt = completed.stream()
                            .map(Session::getEndedAt)
                            .filter(java.util.Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    return FullScenarioActivityDTO.builder()
                            .id(scenario.getId())
                            .name(scenario.getName())
                            .completed(!completed.isEmpty())
                            .lastCompletedAt(lastCompletedAt)
                            .build();
                })
                .toList();
    }
}
