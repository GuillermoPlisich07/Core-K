package com.konverza.scenarios.dto;

import com.konverza.scenarios.entity.Scenario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScenarioExpressRequest {
    @NotBlank private String name;
    @NotNull  private Scenario.Industry industry;
    @NotNull  private Scenario.ClientPersona clientPersona;
    @NotNull  private Scenario.Difficulty difficulty;
    @NotBlank private String productName;
    @NotBlank private String productDescription;
    @NotBlank private String priceRange;
    @NotBlank private String keyDifferentiator;
    @NotBlank private String ownerName;
}
