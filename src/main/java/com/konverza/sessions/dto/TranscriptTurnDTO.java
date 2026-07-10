package com.konverza.sessions.dto;

import com.konverza.sessions.entity.Transcript;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TranscriptTurnDTO {
    private Integer turnNumber;
    private Transcript.Speaker speaker;
    private String text;
    private Long timestampStartMs;
    private Long timestampEndMs;
    private Integer wordCount;
    private BigDecimal speakingRateWpm;
    private Integer pauseBeforeMs;

    // Acústico (librosa)
    private BigDecimal acousticF0Mean;
    private BigDecimal acousticF0Std;
    private BigDecimal acousticJitter;
    private BigDecimal acousticShimmer;
    private BigDecimal acousticHnr;
    private BigDecimal acousticRmsMean;
    private BigDecimal acousticZcrMean;
    private BigDecimal acousticStressScore;

    // Análisis verbal
    private Integer fillerCount;
    private String fillerWords;
    private Integer questionCount;
}
