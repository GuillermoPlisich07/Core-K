package com.konverza.controller;

import com.konverza.dto.SessionCompleteRequest;
import com.konverza.dto.SessionStartRequest;
import com.konverza.entity.Session;
import com.konverza.entity.SessionReport;
import com.konverza.entity.Transcript;
import com.konverza.repository.SessionReportRepository;
import com.konverza.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Sesiones")
public class SessionController {

    private final SessionService sessionService;
    private final SessionReportRepository reportRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea nueva sesion")
    public Session create(@Valid @RequestBody SessionStartRequest req) {
        return sessionService.createSession(req);
    }

    @GetMapping
    @Operation(summary = "Lista sesiones completadas")
    public List<Session> getAll() { return sessionService.findAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una sesion")
    public Session getById(@PathVariable UUID id) { return sessionService.findById(id); }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Completa sesion y genera reporte")
    public Session complete(@PathVariable UUID id, @Valid @RequestBody SessionCompleteRequest req) {
        return sessionService.completeSession(id, req);
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "Retorna el reporte de la sesion")
    public ResponseEntity<SessionReport> getReport(@PathVariable UUID id) {
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
