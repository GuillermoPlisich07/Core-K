package com.konverza.productos.exception;

import java.util.UUID;

public class ServicioNotFoundException extends RuntimeException {
    public ServicioNotFoundException(UUID id) {
        super("Servicio no encontrado: " + id);
    }
}
