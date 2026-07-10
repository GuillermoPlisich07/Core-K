package com.konverza.empresa.exception;

public class EmpresaNotFoundException extends RuntimeException {
    public EmpresaNotFoundException() {
        super("Todavia no se creo el contexto de la empresa");
    }
}
