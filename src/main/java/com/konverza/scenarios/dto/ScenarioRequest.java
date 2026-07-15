package com.konverza.scenarios.dto;

import com.konverza.scenarios.entity.Scenario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ScenarioRequest {
    @NotBlank private String name;
    private String description;
    @NotNull private Scenario.ClientPersona clientPersona;
    @NotNull private Scenario.Difficulty difficulty;
    private String productContext;
    private String systemPrompt;
    private String objectionsGuide;
    private String paymentInfo;
    private String faq;
    private String avatarVoiceId;
    private Scenario.Industry industry;
    private Integer maxDurationMinutes;
    private String evaluationWeights;
    private String forbiddenPhrases;
    // Structured scenario builder fields
    private String vendedorRol;
    private String escenarioObjetivo;
    private UUID empresaId;
    private UUID productoId;
}
