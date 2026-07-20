package com.konverza.productos.controller;

import com.konverza.productos.dto.ProductoRequest;
import com.konverza.productos.entity.Producto;
import com.konverza.productos.service.ProductoService;

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
 * puede ver pero no escribir; Vendedor (EMPLOYEE) puede leer para los 
 * escenarios express (add-rbac-permission-matrix — a diferencia de Escenarios, cuya
 * lectura está abierta a los 3 roles).
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos")
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EXEC','EMPLOYEE')")
    @Operation(summary = "Lista todos los productos")
    public List<Producto> getAll() { return productoService.findAll(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EXEC','EMPLOYEE')")
    @Operation(summary = "Retorna un producto")
    public Producto getById(@PathVariable UUID id) { return productoService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea un nuevo producto")
    public Producto create(@Valid @RequestBody ProductoRequest req) { return productoService.create(req); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edita un producto existente")
    public Producto update(@PathVariable UUID id, @Valid @RequestBody ProductoRequest req) {
        return productoService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina un producto")
    public void delete(@PathVariable UUID id) { productoService.delete(id); }
}
