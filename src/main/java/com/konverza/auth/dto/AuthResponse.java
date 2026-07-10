package com.konverza.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder @AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private long expiresIn;
    private String email;
    private String role;
    private boolean profileCompleted;
}
