package com.konverza.controller;

import com.konverza.dto.RegenerateSectionRequest;
import com.konverza.dto.ScenarioExpressRequest;
import com.konverza.dto.ScenarioRequest;
import com.konverza.entity.Scenario;
import com.konverza.service.ScenarioExpressService;
import com.konverza.service.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @Operation(summary = "Crea nuevo escenario (flujo Detallado)")
    public Scenario create(@Valid @RequestBody ScenarioRequest req) { return scenarioService.create(req); }

    @PostMapping("/express")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea escenario con generación IA (flujo Express)")
    public Scenario createExpress(@Valid @RequestBody ScenarioExpressRequest req) {
        return scenarioExpressService.generateAndSave(req);
    }

    @PostMapping("/{id}/regenerate-section")
    @Operation(summary = "Regenera una sección del escenario con IA")
    public Map<String, String> regenerateSection(
            @PathVariable UUID id,
            @Valid @RequestBody RegenerateSectionRequest req) {
        String content = scenarioExpressService.regenerateSection(id, req.getSection());
        return Map.of("section", req.getSection(), "content", content);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita escenario existente")
    public Scenario update(@PathVariable UUID id, @Valid @RequestBody ScenarioRequest req) {
        return scenarioService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina escenario")
    public void delete(@PathVariable UUID id) { scenarioService.delete(id); }
}
