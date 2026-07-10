package com.konverza.auth.controller;

import com.konverza.auth.dto.AuthResponse;
import com.konverza.auth.dto.LoginRequest;
import com.konverza.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Value("${app.cookie-secure:true}")
    private boolean cookieSecure;

    @PostMapping("/login")
    @Operation(summary = "Autentica un usuario y crea una sesion")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return withRefreshCookie(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva el access token usando el refresh token")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        return withRefreshCookie(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cierra la sesion e invalida el refresh token")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthService.AuthResult result) {
        ResponseCookie cookie = buildCookie(result.refreshToken(), refreshTokenExpirationMs / 1000);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.response());
    }

    private ResponseCookie buildCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}
