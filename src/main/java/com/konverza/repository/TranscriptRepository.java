package com.konverza.repository;

import com.konverza.entity.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {
    List<Transcript> findBySessionIdOrderByTurnNumber(UUID sessionId);
}
