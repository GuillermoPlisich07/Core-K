package com.konverza.auth.dto;

import com.konverza.auth.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateUserRequest {

    @NotNull(message = "El rol es obligatorio")
    private User.Role role;

    @NotNull(message = "El estado habilitado/deshabilitado es obligatorio")
    private Boolean enabled;
}
