package com.konverza.productos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServicioRequest {
    @NotBlank private String name;
    private String description;
    private String context;
}
