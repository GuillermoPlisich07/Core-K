package com.konverza.sessions.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "biometric_samples")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BiometricSample {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "timestamp_ms")
    private Long timestampMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(columnDefinition = "TEXT")
    private String emotions;

    @Column(name = "dominant_emotion")
    private String dominantEmotion;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "eye_contact")
    private Boolean eyeContact;

    @Column(name = "head_orientation", columnDefinition = "TEXT")
    private String headOrientation;

    // ── MediaPipe Tasks API ───────────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String blendshapes;

    @Column(name = "head_yaw", precision = 6, scale = 2)
    private BigDecimal headYaw;

    @Column(name = "head_pitch", precision = 6, scale = 2)
    private BigDecimal headPitch;

    @Column(name = "head_roll", precision = 6, scale = 2)
    private BigDecimal headRoll;

    @Column(name = "eye_openness", precision = 4, scale = 3)
    private BigDecimal eyeOpenness;

    @Column(name = "blink_detected")
    private Boolean blinkDetected;

    @Column(name = "smile_intensity", precision = 4, scale = 3)
    private BigDecimal smileIntensity;

    @Column(name = "brow_furrow", precision = 4, scale = 3)
    private BigDecimal browFurrow;

    @Column(name = "mouth_open")
    private Boolean mouthOpen;

    @Column(name = "face_detected")
    private Boolean faceDetected;

    // ── Índices compuestos ────────────────────────────────────────────────────
    @Column(name = "confidence_index", precision = 4, scale = 2)
    private BigDecimal confidenceIndex;

    @Column(name = "stress_index", precision = 4, scale = 2)
    private BigDecimal stressIndex;

    @Column(name = "engagement_index", precision = 4, scale = 2)
    private BigDecimal engagementIndex;

    // ── Acústico (source=VOICE_ACOUSTIC) ─────────────────────────────────────
    @Column(name = "acoustic_features", columnDefinition = "TEXT")
    private String acousticFeatures;

    @Column(name = "acoustic_stress_score", precision = 4, scale = 2)
    private BigDecimal acousticStressScore;

    public enum Source { VOICE, FACE, VOICE_ACOUSTIC }
}
