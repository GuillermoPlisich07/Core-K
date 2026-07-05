package com.konverza.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pronunciation_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PronunciationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    @Column(name = "accuracy_score", precision = 5, scale = 2)
    private BigDecimal accuracyScore;

    @Column(name = "fluency_score", precision = 5, scale = 2)
    private BigDecimal fluencyScore;

    @Column(name = "completeness_score", precision = 5, scale = 2)
    private BigDecimal completenessScore;

    @Column(name = "prosody_score", precision = 5, scale = 2)
    private BigDecimal prosodyScore;

    @Column(name = "words_detail", columnDefinition = "TEXT")
    private String wordsDetail;

    @Column(name = "phonemes_detail", columnDefinition = "TEXT")
    private String phonemesDetail;
}
