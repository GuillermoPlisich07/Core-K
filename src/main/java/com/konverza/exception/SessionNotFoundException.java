package com.konverza.exception;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(UUID id) {
        super("Sesion no encontrada: " + id);
    }
}
