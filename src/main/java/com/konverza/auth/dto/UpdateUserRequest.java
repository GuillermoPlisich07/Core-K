package com.konverza.auth.dto;

import com.konverza.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * firstName, lastName, email, and password are all optional on update — only
 * the fields present are changed (see UserManagementService#update). role and
 * enabled remain required, matching the screen's existing always-shown
 * role/status controls.
 */
@Getter @Setter
public class UpdateUserRequest {

    private String firstName;

    private String lastName;

    @Email(message = "El email no tiene un formato valido")
    private String email;

    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private User.Role role;

    @NotNull(message = "El estado habilitado/deshabilitado es obligatorio")
    private Boolean enabled;
}
