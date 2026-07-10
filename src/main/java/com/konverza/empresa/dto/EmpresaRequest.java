package com.konverza.empresa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmpresaRequest {
    @NotBlank private String name;
    private String context;
}
