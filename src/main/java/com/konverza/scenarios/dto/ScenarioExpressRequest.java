package com.konverza.scenarios.dto;

import com.konverza.scenarios.entity.Scenario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ScenarioExpressRequest {
    @NotBlank private String name;
    private String description;
    @NotNull  private Scenario.ClientPersona clientPersona;
    @NotNull  private Scenario.Difficulty difficulty;
    @NotNull  private UUID productoId;
    private String vendedorRol;
}
