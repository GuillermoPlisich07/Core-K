package com.konverza.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Sesion invalida o expirada. Volve a iniciar sesion.");
    }
}
