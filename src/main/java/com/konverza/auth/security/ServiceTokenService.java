package com.konverza.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues short-lived delegated session tokens that Web-k presents directly to
 * AI-Service-k (add-service-auth). Signed with a secret shared between Core-k
 * and AI-Service-k, distinct from JwtService's user-facing signing key.
 */
@Slf4j
@Component
public class ServiceTokenService {

    private static final String ISSUER = "core-k";
    private static final String AUDIENCE = "ai-service-k";

    // No default value on purpose: Spring fails to start if this is unset,
    // consistent with "Secrets are never hardcoded" for service-to-service auth.
    @Value("${service.jwt.secret}")
    private String serviceJwtSecret;

    @Value("${service.jwt.delegated-token-expiration-ms:7200000}")
    private long delegatedTokenExpirationMs;

    private SecretKey signingKey;

    private SecretKey signingKey() {
        if (signingKey == null) {
            signingKey = Keys.hmacShaKeyFor(serviceJwtSecret.getBytes(StandardCharsets.UTF_8));
        }
        return signingKey;
    }

    public String mintDelegatedToken(UUID userId, String role, UUID scenarioId, UUID sessionId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(delegatedTokenExpirationMs);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("scenario_id", scenarioId.toString())
                .claim("session_id", sessionId.toString())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }
}
