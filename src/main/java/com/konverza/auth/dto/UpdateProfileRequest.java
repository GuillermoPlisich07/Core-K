package com.konverza.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateProfileRequest {

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 16, message = "La edad debe ser al menos 16")
    @Max(value = 120, message = "La edad no puede superar 120")
    private Integer age;

    @NotBlank(message = "La personalidad es obligatoria")
    private String personality;

    @NotBlank(message = "La autodescripcion es obligatoria")
    private String selfDescription;
}
