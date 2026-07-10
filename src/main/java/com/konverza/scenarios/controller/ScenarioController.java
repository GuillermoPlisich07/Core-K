package com.konverza.scenarios.controller;

import com.konverza.scenarios.dto.RegenerateSectionRequest;
import com.konverza.scenarios.dto.ScenarioExpressRequest;
import com.konverza.scenarios.dto.ScenarioRequest;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.service.ScenarioExpressService;
import com.konverza.scenarios.service.ScenarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
@Tag(name = "Escenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final ScenarioExpressService scenarioExpressService;

    @GetMapping
    @Operation(summary = "Lista todos los escenarios")
    public List<Scenario> getAll() { return scenarioService.findAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Retorna escenario completo")
    public Scenario getById(@PathVariable UUID id) { return scenarioService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea nuevo escenario (flujo Detallado)")
    public Scenario create(@Valid @RequestBody ScenarioRequest req) { return scenarioService.create(req); }

    @PostMapping("/express")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    @Operation(summary = "Crea escenario con generación IA (flujo Express) — EMPLOYEE crea para su propia práctica; el flujo Detallado (largo) sigue siendo ADMIN-only")
    public Scenario createExpress(@Valid @RequestBody ScenarioExpressRequest req) {
        return scenarioExpressService.generateAndSave(req);
    }

    @PostMapping("/{id}/regenerate-section")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    @Operation(summary = "Regenera una sección del escenario con IA — usado tanto por el flujo Express (EMPLOYEE) como por el Detallado (ADMIN)")
    public Map<String, String> regenerateSection(
            @PathVariable UUID id,
            @Valid @RequestBody RegenerateSectionRequest req) {
        String content = scenarioExpressService.regenerateSection(id, req.getSection());
        return Map.of("section", req.getSection(), "content", content);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    @Operation(summary = "Edita escenario existente — ScenarioService.update enforces that EMPLOYEE may only save their own Express-created scenarios; ADMIN can edit any")
    public Scenario update(@PathVariable UUID id, @Valid @RequestBody ScenarioRequest req) {
        return scenarioService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina escenario")
    public void delete(@PathVariable UUID id) { scenarioService.delete(id); }
}
