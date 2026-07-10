package com.konverza.auth.service;

import com.konverza.auth.dto.LoginRequest;
import com.konverza.auth.entity.RefreshToken;
import com.konverza.auth.entity.User;
import com.konverza.auth.exception.AccountDisabledException;
import com.konverza.auth.exception.InvalidCredentialsException;
import com.konverza.auth.exception.InvalidRefreshTokenException;
import com.konverza.auth.repository.RefreshTokenRepository;
import com.konverza.auth.repository.UserRepository;
import com.konverza.auth.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604800000L);
    }

    private User buildUser(boolean enabled) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("vendedor@konverza.com")
                .passwordHash("hashed")
                .role(User.Role.EMPLOYEE)
                .enabled(enabled)
                .build();
    }

    private LoginRequest buildRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    @Test
    @DisplayName("login with valid credentials issues an access token and role")
    void login_validCredentials_issuesSession() {
        User user = buildUser(true);
        when(userRepository.findByEmailIgnoreCase("vendedor@konverza.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user.getId(), "EMPLOYEE")).thenReturn("signed.jwt.token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthService.AuthResult result = authService.login(buildRequest("vendedor@konverza.com", "correct-password"));

        assertThat(result.response().getAccessToken()).isEqualTo("signed.jwt.token");
        assertThat(result.response().getRole()).isEqualTo("employee");
        assertThat(result.response().getEmail()).isEqualTo("vendedor@konverza.com");
        assertThat(result.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login with unknown email throws InvalidCredentialsException")
    void login_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmailIgnoreCase("ghost@konverza.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(buildRequest("ghost@konverza.com", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("login with wrong password throws InvalidCredentialsException")
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = buildUser(true);
        when(userRepository.findByEmailIgnoreCase("vendedor@konverza.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(buildRequest("vendedor@konverza.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("login for a disabled account throws AccountDisabledException")
    void login_disabledAccount_throwsAccountDisabled() {
        User user = buildUser(false);
        when(userRepository.findByEmailIgnoreCase("vendedor@konverza.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(buildRequest("vendedor@konverza.com", "correct-password")))
                .isInstanceOf(AccountDisabledException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("refresh with a missing token throws InvalidRefreshTokenException")
    void refresh_missingToken_throwsInvalidRefreshToken() {
        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.refresh(""))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("refresh with an unknown token hash throws InvalidRefreshTokenException")
    void refresh_unknownToken_throwsInvalidRefreshToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("some-raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("refresh with an expired token throws InvalidRefreshTokenException")
    void refresh_expiredToken_throwsInvalidRefreshToken() {
        User user = buildUser(true);
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("some-raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("refresh with a revoked token throws InvalidRefreshTokenException")
    void refresh_revokedToken_throwsInvalidRefreshToken() {
        User user = buildUser(true);
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("some-raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("refresh for a disabled account throws AccountDisabledException and revokes the old token")
    void refresh_disabledAccount_throwsAccountDisabled() {
        User user = buildUser(false);
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("some-raw-token"))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    @DisplayName("logout with a valid token revokes it")
    void logout_validToken_revokesStoredToken() {
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hash")
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        authService.logout("some-raw-token");

        verify(refreshTokenRepository).save(argThat(RefreshToken::isRevoked));
    }

    @Test
    @DisplayName("logout with no token is a no-op")
    void logout_missingToken_doesNothing() {
        authService.logout(null);
        authService.logout("");

        verifyNoInteractions(refreshTokenRepository);
    }
}
