package com.konverza.scenarios.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetEnabledRequest {
    @NotNull private Boolean enabled;

    public boolean isEnabled() { return Boolean.TRUE.equals(enabled); }
}
