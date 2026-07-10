package com.konverza.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Reads the authenticated principal set by {@link JwtAuthenticationFilter}.
 * Shared by every service that needs "who's calling" for own-vs-all scoping
 * or role-dependent business rules (add-rbac-permission-matrix) — role gating
 * on the endpoint itself belongs in {@code @PreAuthorize}, not here.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static UUID id() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName());
    }

    public static String role() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("EMPLOYEE");
    }
}
