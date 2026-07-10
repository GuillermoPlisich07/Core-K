package com.konverza.sessions.repository;

import com.konverza.sessions.entity.Session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findAllByOrderByStartedAtDesc();
    List<Session> findAllByUserIdOrderByStartedAtDesc(UUID userId);
    List<Session> findAllByUserIsNull();
}
