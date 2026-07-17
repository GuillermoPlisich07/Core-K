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
}
