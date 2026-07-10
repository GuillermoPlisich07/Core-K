package com.konverza.sessions.dto;

import lombok.Data;
import java.util.List;

@Data
public class SessionCompleteRequest {
    private Integer durationSeconds;
    private Integer totalTurns;
    private List<TranscriptTurnDTO> transcript;
    private List<BiometricSampleDTO> biometricSamples;
    private PronunciationDTO pronunciation;
    private String verbalAnalysis;
    private String biometricSummary;
    private String vendorName;
    private String scenarioName;
    private String ntcMetrics;  // JSON snapshot del Natural Turn Controller
}
