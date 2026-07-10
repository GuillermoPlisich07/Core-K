package com.konverza.sessions.repository;

import com.konverza.sessions.entity.BiometricSample;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BiometricSampleRepository extends JpaRepository<BiometricSample, UUID> {
    List<BiometricSample> findBySessionIdOrderByTimestampMs(UUID sessionId);
}
