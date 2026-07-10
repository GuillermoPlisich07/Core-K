package com.konverza.scenarios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scenarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_persona", nullable = false)
    private ClientPersona clientPersona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "product_context", columnDefinition = "TEXT")
    private String productContext;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "objections_guide", columnDefinition = "TEXT")
    private String objectionsGuide;

    @Column(name = "payment_info", columnDefinition = "TEXT")
    private String paymentInfo;

    @Column(columnDefinition = "TEXT")
    private String faq;

    @Column(name = "avatar_voice_id")
    private String avatarVoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "industry")
    private Industry industry;

    @Column(name = "max_duration_minutes")
    private Integer maxDurationMinutes;

    @Column(name = "evaluation_weights", columnDefinition = "TEXT")
    private String evaluationWeights;

    @Column(name = "forbidden_phrases", columnDefinition = "TEXT")
    private String forbiddenPhrases;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "owner_name")
    private String ownerName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ClientPersona { ANGRY, DIFFICULT, INDIFFERENT, DEMANDING }
    public enum Difficulty { EASY, MEDIUM, HARD }
    public enum Industry { SOFTWARE_B2B, FINANZAS, CONSULTORIA, TELCO, SEGUROS, RETAIL, SALUD, OTRO }
}
