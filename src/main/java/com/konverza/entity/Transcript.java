package com.konverza.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transcripts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transcript {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "turn_number", nullable = false)
    private Integer turnNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Speaker speaker;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "timestamp_start_ms")
    private Long timestampStartMs;

    @Column(name = "timestamp_end_ms")
    private Long timestampEndMs;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "speaking_rate_wpm", precision = 6, scale = 2)
    private BigDecimal speakingRateWpm;

    @Column(name = "pause_before_ms")
    private Integer pauseBeforeMs;

    // ── Análisis acústico (librosa, async por turno) ──────────────────────────
    @Column(name = "acoustic_f0_mean", precision = 8, scale = 2)
    private BigDecimal acousticF0Mean;

    @Column(name = "acoustic_f0_std", precision = 8, scale = 2)
    private BigDecimal acousticF0Std;

    @Column(name = "acoustic_jitter", precision = 8, scale = 4)
    private BigDecimal acousticJitter;

    @Column(name = "acoustic_shimmer", precision = 8, scale = 4)
    private BigDecimal acousticShimmer;

    @Column(name = "acoustic_hnr", precision = 8, scale = 2)
    private BigDecimal acousticHnr;

    @Column(name = "acoustic_rms_mean", precision = 10, scale = 6)
    private BigDecimal acousticRmsMean;

    @Column(name = "acoustic_zcr_mean", precision = 10, scale = 6)
    private BigDecimal acousticZcrMean;

    @Column(name = "acoustic_stress_score", precision = 4, scale = 2)
    private BigDecimal acousticStressScore;

    // ── Análisis verbal ───────────────────────────────────────────────────────
    @Column(name = "filler_count")
    private Integer fillerCount;

    @Column(name = "filler_words", columnDefinition = "TEXT")
    private String fillerWords;

    @Column(name = "sentiment_score", precision = 5, scale = 3)
    private BigDecimal sentimentScore;

    @Column(name = "question_count")
    private Integer questionCount;

    @Column(name = "objection_handled")
    private Boolean objectionHandled;

    public enum Speaker { VENDOR, CLIENT }
}
