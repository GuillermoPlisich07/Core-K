package com.konverza.repository;

import com.konverza.entity.PronunciationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PronunciationResultRepository extends JpaRepository<PronunciationResult, UUID> {
    Optional<PronunciationResult> findBySessionId(UUID sessionId);
}
