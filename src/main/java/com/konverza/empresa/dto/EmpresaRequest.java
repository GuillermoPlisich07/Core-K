package com.konverza.empresa.dto;

import com.konverza.shared.enums.Industry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class EmpresaRequest {
    @NotBlank private String name;
    private String context;
    private String description;
    private String vision;
    private String objective;

    @NotEmpty(message = "Seleccioná al menos una industria")
    private Set<Industry> industries;
}
