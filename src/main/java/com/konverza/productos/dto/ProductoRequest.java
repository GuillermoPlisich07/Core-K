package com.konverza.productos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class ProductoRequest {
    @NotBlank private String name;
    private String description;
    private String context;
    private String priceRange;
    private String keyDifferentiator;
    private String paymentInfo;
    private Set<String> tags;
}
