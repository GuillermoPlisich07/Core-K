package com.konverza.productos.controller;

import com.konverza.productos.dto.ServicioRequest;
import com.konverza.productos.entity.Servicio;
import com.konverza.productos.service.ServicioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestión de servicios — mismas reglas de rol que ProductoController
 * (add-rbac-permission-matrix).
 */
@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
@Tag(name = "Servicios")
public class ServicioController {

    private final ServicioService servicioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EXEC')")
    @Operation(summary = "Lista todos los servicios")
    public List<Servicio> getAll() { return servicioService.findAll(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EXEC')")
    @Operation(summary = "Retorna un servicio")
    public Servicio getById(@PathVariable UUID id) { return servicioService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea un nuevo servicio")
    public Servicio create(@Valid @RequestBody ServicioRequest req) { return servicioService.create(req); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edita un servicio existente")
    public Servicio update(@PathVariable UUID id, @Valid @RequestBody ServicioRequest req) {
        return servicioService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina un servicio")
    public void delete(@PathVariable UUID id) { servicioService.delete(id); }
}
