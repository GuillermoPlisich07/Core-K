package com.konverza.auth.exception;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("La cuenta esta deshabilitada. Contacta a un administrador.");
    }
}
