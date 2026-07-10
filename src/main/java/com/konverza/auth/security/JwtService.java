package com.konverza.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtService {

    private static final String INSECURE_DEV_SECRET =
            "dev-only-insecure-jwt-secret-change-me-before-any-real-deployment";

    @Value("${jwt.secret:}")
    private String configuredSecret;

    @Value("${jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = configuredSecret;
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET no esta configurado: usando una clave insegura de desarrollo. " +
                    "Configura la variable de entorno JWT_SECRET antes de desplegar en cualquier " +
                    "ambiente compartido o de produccion.");
            secret = INSECURE_DEV_SECRET;
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpirationMs);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Throws io.jsonwebtoken.JwtException (or a subclass, e.g. ExpiredJwtException,
     * SignatureException) if the token is missing, malformed, expired, or has an
     * invalid signature. Callers should let it propagate to the auth filter/handler.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}
