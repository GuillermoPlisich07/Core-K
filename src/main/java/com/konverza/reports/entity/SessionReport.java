package com.konverza.reports.entity;

import com.konverza.sessions.entity.Session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "scenario_name")
    private String scenarioName;

    @Column(name = "verbal_analysis", columnDefinition = "TEXT")
    private String verbalAnalysis;

    @Column(name = "biometric_summary", columnDefinition = "TEXT")
    private String biometricSummary;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "score_persuasion", precision = 4, scale = 2)
    private BigDecimal scorePersuasion;

    @Column(name = "score_confidence", precision = 4, scale = 2)
    private BigDecimal scoreConfidence;

    @Column(name = "score_product_knowledge", precision = 4, scale = 2)
    private BigDecimal scoreProductKnowledge;

    @Column(name = "score_objection_handling", precision = 4, scale = 2)
    private BigDecimal scoreObjectionHandling;

    @Column(name = "score_pronunciation", precision = 4, scale = 2)
    private BigDecimal scorePronunciation;

    // ── Métricas de habla (Gong Labs benchmarks) ──────────────────────────────
    @Column(name = "talk_ratio", precision = 5, scale = 2)
    private BigDecimal talkRatio;

    @Column(name = "avg_speaking_rate_wpm", precision = 6, scale = 2)
    private BigDecimal avgSpeakingRateWpm;

    @Column(name = "longest_monologue_sec")
    private Integer longestMonologueSec;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "open_questions_ratio", precision = 5, scale = 2)
    private BigDecimal openQuestionsRatio;

    @Column(name = "product_terms_coverage", precision = 5, scale = 2)
    private BigDecimal productTermsCoverage;

    @Column(name = "filler_rate", precision = 5, scale = 2)
    private BigDecimal fillerRate;

    @Column(name = "top_fillers", columnDefinition = "TEXT")
    private String topFillers;

    @Column(name = "negative_phrases", columnDefinition = "TEXT")
    private String negativePhrases;

    @Column(name = "lexical_diversity", precision = 5, scale = 3)
    private BigDecimal lexicalDiversity;

    @Column(name = "objection_handling_rate", precision = 5, scale = 2)
    private BigDecimal objectionHandlingRate;

    @Column(name = "consistency_score", precision = 5, scale = 3)
    private BigDecimal consistencyScore;

    // ── Métricas faciales ─────────────────────────────────────────────────────
    @Column(name = "avg_confidence_index", precision = 4, scale = 2)
    private BigDecimal avgConfidenceIndex;

    @Column(name = "avg_stress_index", precision = 4, scale = 2)
    private BigDecimal avgStressIndex;

    @Column(name = "avg_engagement_index", precision = 4, scale = 2)
    private BigDecimal avgEngagementIndex;

    @Column(name = "eye_contact_ratio", precision = 5, scale = 2)
    private BigDecimal eyeContactRatio;

    @Column(name = "smile_ratio", precision = 5, scale = 2)
    private BigDecimal smileRatio;

    @Column(name = "brow_furrow_ratio", precision = 5, scale = 2)
    private BigDecimal browFurrowRatio;

    @Column(name = "avg_blink_rate", precision = 5, scale = 2)
    private BigDecimal avgBlinkRate;

    @Column(name = "looking_away_events")
    private Integer lookingAwayEvents;

    // ── Acústicas consolidadas ────────────────────────────────────────────────
    @Column(name = "avg_f0_mean", precision = 8, scale = 2)
    private BigDecimal avgF0Mean;

    @Column(name = "avg_jitter", precision = 8, scale = 4)
    private BigDecimal avgJitter;

    @Column(name = "avg_hnr", precision = 8, scale = 2)
    private BigDecimal avgHnr;

    @Column(name = "acoustic_stress_score", precision = 4, scale = 2)
    private BigDecimal acousticStressScore;

    // ── Línea temporal emocional (Hume batch — v2) ────────────────────────────
    @Column(name = "emotion_timeline", columnDefinition = "TEXT")
    private String emotionTimeline;

    @Column(name = "peak_stress_turn")
    private Integer peakStressTurn;

    @Column(name = "peak_confidence_turn")
    private Integer peakConfidenceTurn;

    @Column(name = "turn_suggestions", columnDefinition = "TEXT")
    private String turnSuggestions;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}
