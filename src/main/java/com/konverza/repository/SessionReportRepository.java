package com.konverza.repository;

import com.konverza.entity.SessionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionReportRepository extends JpaRepository<SessionReport, UUID> {
    Optional<SessionReport> findBySessionId(UUID sessionId);
}
