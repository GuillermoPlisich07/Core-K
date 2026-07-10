package com.konverza.sessions.entity;

import com.konverza.auth.entity.User;
import com.konverza.scenarios.entity.Scenario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    /**
     * Owner of the session, set from the authenticated principal at creation
     * (add-rbac-permission-matrix). Nullable because pre-existing rows have no
     * reliable name-to-user mapping (User has no display-name field to backfill
     * against) — see SessionUserBackfill. Rows left null are only visible to
     * ADMIN/EXEC, never attributed to an EMPLOYEE's personal session list.
     * EAGER (like scenario below) because spring.jpa.open-in-view=false means
     * a LAZY proxy can't be resolved once serialization happens outside the
     * request's Hibernate session.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "total_turns")
    private Integer totalTurns;

    @Column(name = "overall_score", precision = 4, scale = 2)
    private BigDecimal overallScore;

    /**
     * Delegated token for Web-k to present to AI-Service-k (add-service-auth).
     * Response-only: not persisted, only populated by SessionService.createSession.
     */
    @Transient
    private String delegatedToken;

    public enum Status { ACTIVE, COMPLETED, ABANDONED }
}
