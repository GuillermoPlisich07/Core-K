package com.konverza.scenarios.repository;

import com.konverza.scenarios.entity.Scenario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, UUID> {
    boolean existsByName(String name);
}
