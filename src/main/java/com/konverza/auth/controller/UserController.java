package com.konverza.auth.controller;

import com.konverza.auth.dto.CreateUserRequest;
import com.konverza.auth.dto.UpdateProfileRequest;
import com.konverza.auth.dto.UpdateUserRequest;
import com.konverza.auth.entity.User;
import com.konverza.auth.security.CurrentUser;
import com.konverza.auth.service.UserManagementService;

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
 * Gestion de usuarios — Administrador-only en toda la matriz de permisos
 * (add-rbac-permission-matrix): crear/editar/eliminar/cambiar rol son
 * exclusivos de ADMIN; Autoridad (EXEC) solo puede listar (solo lectura).
 * Los endpoints {@code /me}* son la excepcion: cualquier rol autenticado
 * puede leer/editar su propia fila (add-users-empresa-profile), separados de
 * los endpoints Admin-only que actuan sobre cualquier usuario por id.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios")
public class UserController {

    private final UserManagementService userManagementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EXEC')")
    @Operation(summary = "Lista todos los usuarios")
    public List<User> getAll() { return userManagementService.findAll(); }

    @GetMapping("/me")
    @Operation(summary = "Retorna el perfil del usuario autenticado")
    public User getOwnProfile() { return userManagementService.findById(CurrentUser.id()); }

    @PutMapping("/me/profile")
    @Operation(summary = "Completa o edita el perfil (edad, personalidad, autodescripcion) del usuario autenticado")
    public User updateOwnProfile(@Valid @RequestBody UpdateProfileRequest req) {
        return userManagementService.updateOwnProfile(CurrentUser.id(), req);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea un nuevo usuario")
    public User create(@Valid @RequestBody CreateUserRequest req) {
        return userManagementService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edita rol/estado de un usuario existente")
    public User update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        return userManagementService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina un usuario")
    public void delete(@PathVariable UUID id) { userManagementService.delete(id); }
}
