package com.konverza.auth.service;

import com.konverza.auth.dto.AuthResponse;
import com.konverza.auth.dto.LoginRequest;
import com.konverza.auth.entity.RefreshToken;
import com.konverza.auth.entity.User;
import com.konverza.auth.exception.AccountDisabledException;
import com.konverza.auth.exception.InvalidCredentialsException;
import com.konverza.auth.exception.InvalidRefreshTokenException;
import com.konverza.auth.repository.RefreshTokenRepository;
import com.konverza.auth.repository.UserRepository;
import com.konverza.auth.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public AuthResult login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }

        return issueSession(user);
    }

    @Transactional
    public AuthResult refresh(String presentedRefreshToken) {
        RefreshToken stored = validateRefreshToken(presentedRefreshToken);
        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueSession(user);
    }

    @Transactional
    public void logout(String presentedRefreshToken) {
        if (presentedRefreshToken == null || presentedRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(presentedRefreshToken))
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    private RefreshToken validateRefreshToken(String presentedRefreshToken) {
        if (presentedRefreshToken == null || presentedRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(presentedRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }
        return stored;
    }

    private AuthResult issueSession(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = issueRefreshToken(user);
        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .email(user.getEmail())
                .role(user.getRole().name().toLowerCase())
                .profileCompleted(user.isProfileCompleted())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .build();
        return new AuthResult(response, refreshToken);
    }

    private String issueRefreshToken(User user) {
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs)))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }

    public record AuthResult(AuthResponse response, String refreshToken) {}
}
