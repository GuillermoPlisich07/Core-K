package com.konverza.auth.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("Usuario no encontrado: " + id);
    }
}
