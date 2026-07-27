package com.konverza.sessions.controller;

import com.konverza.reports.entity.SessionReport;
import com.konverza.reports.repository.SessionReportRepository;
import com.konverza.sessions.dto.SessionCompleteRequest;
import com.konverza.sessions.dto.SessionStartRequest;
import com.konverza.sessions.entity.Session;
import com.konverza.sessions.entity.Transcript;
import com.konverza.sessions.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controla el ciclo de vida de las sesiones de entrenamiento.
 *
 * Roles de usuario humanos (User.Role):
 * - EMPLOYEE (Empleado/Vendedor): Crea e inicia simulaciones; ve solo sus propios reportes.
 * - ADMIN (Administrador): Crea simulaciones; ve todas las sesiones y reportes.
 * - EXEC (Ejecutivo/Autoridad): No crea simulaciones por regla de negocio; supervisa reportes y dashboards.
 *
 * Rol de infraestructura / sistema (M2M):
 * - SERVICE (ROLE_SERVICE): Asignado a AI-Service-k mediante X-Service-Key para completar la sesión y sincronizar métricas.
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Sesiones")
public class SessionController {

    private final SessionService sessionService;
    private final SessionReportRepository reportRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    @Operation(summary = "Crea nueva sesion")
    public Session create(@Valid @RequestBody SessionStartRequest req) {
        return sessionService.createSession(req);
    }

    @GetMapping
    @Operation(summary = "Lista sesiones (propias para EMPLOYEE, todas para ADMIN/EXEC)")
    public List<Session> getAll() { return sessionService.findAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una sesion")
    public Session getById(@PathVariable UUID id) { return sessionService.findById(id); }

    /**
     * Permite completar una sesión y desencadenar la generación del reporte.
     * Permitido para EMPLOYEE/ADMIN (fallback usuario) y SERVICE (AI-Service-k vía X-Service-Key).
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SERVICE')")
    @Operation(summary = "Completa sesion y genera reporte")
    public Session complete(@PathVariable UUID id, @Valid @RequestBody SessionCompleteRequest req) {
        return sessionService.completeSession(id, req);
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "Retorna el reporte de la sesion")
    public ResponseEntity<SessionReport> getReport(@PathVariable UUID id) {
        sessionService.findById(id); // enforces own-vs-all access before returning the report
        return reportRepository.findBySessionId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/transcript")
    @Operation(summary = "Retorna la transcripcion de la sesion")
    public List<Transcript> getTranscript(@PathVariable UUID id) {
        return sessionService.getTranscript(id);
    }
}
