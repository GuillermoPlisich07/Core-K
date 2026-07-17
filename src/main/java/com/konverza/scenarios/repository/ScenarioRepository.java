package com.konverza.scenarios.repository;

import com.konverza.scenarios.entity.Scenario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, UUID> {
    boolean existsByName(String name);

    /** Backs ScenarioExpirationJob's 2-month auto-disable (scenario-privacy-and-lifecycle). */
    List<Scenario> findByCreatedByAndEnabledTrueAndCreatedAtBefore(String createdBy, LocalDateTime cutoff);

    /**
     * Backs the per-user activity panel's quick-scenario list
     * (user-activity-detail-panel) — deliberately bypasses
     * ScenarioService.isVisibleToCurrentUser's creator-only scoping; only
     * ever called from the ADMIN/EXEC-gated activity endpoint, never from a
     * general scenario listing path.
     */
    List<Scenario> findByCreatedByUser_IdAndCreatedBy(UUID userId, String createdBy);

    /** Current company-wide enabled Escenario Completo catalog (user-activity-detail-panel). */
    List<Scenario> findByCreatedByAndEnabledTrue(String createdBy);
}
