package com.konverza.scenarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScenarioPhaseRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private Integer orderIndex;
    private Integer estimatedTimeMinutes;
}
