package com.konverza.auth.exception;

public class InvalidCurrentPasswordException extends RuntimeException {
    public InvalidCurrentPasswordException() {
        super("La contrasena actual ingresada es incorrecta");
    }
}
