package com.konverza.empresa.controller;

import com.konverza.empresa.dto.EmpresaRequest;
import com.konverza.empresa.entity.Empresa;
import com.konverza.empresa.service.EmpresaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contexto de empresa — registro singleton (add-users-empresa-profile).
 * Lectura ADMIN/EXEC, escritura ADMIN-only, igual que Productos/Servicios.
 */
@RestController
@RequestMapping("/api/empresa")
@RequiredArgsConstructor
@Tag(name = "Empresa")
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EXEC','EMPLOYEE')")
    @Operation(summary = "Retorna el contexto de la empresa (404 si todavia no existe)")
    public Empresa get() { return empresaService.get(); }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea o edita el contexto de la empresa (upsert)")
    public Empresa upsert(@Valid @RequestBody EmpresaRequest req) {
        return empresaService.upsert(req);
    }
}
