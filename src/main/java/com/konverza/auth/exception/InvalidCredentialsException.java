package com.konverza.auth.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Email o contrasena incorrectos");
    }
}
