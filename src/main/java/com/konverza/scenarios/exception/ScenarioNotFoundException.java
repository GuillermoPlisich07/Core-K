package com.konverza.scenarios.exception;

import java.util.UUID;

/**
 * Thrown both when a scenario truly doesn't exist and when it exists but
 * isn't visible to the current user (e.g. another user's Escenario Rápido) —
 * both cases resolve to 404, so a non-owner can't distinguish "doesn't
 * exist" from "not yours" (scenario-privacy-and-lifecycle).
 */
public class ScenarioNotFoundException extends RuntimeException {
    public ScenarioNotFoundException(UUID id) {
        super("Escenario no encontrado: " + id);
    }
}
