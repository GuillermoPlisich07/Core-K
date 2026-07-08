package com.konverza.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegenerateSectionRequest {
    @NotBlank private String section;
}
