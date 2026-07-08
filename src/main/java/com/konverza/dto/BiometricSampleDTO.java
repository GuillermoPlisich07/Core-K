package com.konverza.dto;

import com.konverza.entity.BiometricSample;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BiometricSampleDTO {
    private Long timestampMs;
    private BiometricSample.Source source;
    private String emotions;
    private String dominantEmotion;
    private BigDecimal confidence;
    private Boolean eyeContact;
    private String headOrientation;

    // MediaPipe Tasks API
    private BigDecimal headYaw;
    private BigDecimal headPitch;
    private BigDecimal headRoll;
    private BigDecimal eyeOpenness;
    private Boolean blinkDetected;
    private BigDecimal smileIntensity;
    private BigDecimal browFurrow;
    private Boolean mouthOpen;
    private Boolean faceDetected;
    private BigDecimal confidenceIndex;
    private BigDecimal stressIndex;
    private BigDecimal engagementIndex;

    // Acústico (source=VOICE_ACOUSTIC)
    private String acousticFeatures;
    private BigDecimal acousticStressScore;
}
