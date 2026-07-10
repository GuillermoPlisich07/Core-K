package com.konverza.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTokenServiceTest {

    private static final String SECRET = "test-only-service-jwt-signing-secret-not-for-any-real-use";

    private ServiceTokenService serviceTokenService;

    @BeforeEach
    void setUp() {
        serviceTokenService = new ServiceTokenService();
        ReflectionTestUtils.setField(serviceTokenService, "serviceJwtSecret", SECRET);
        ReflectionTestUtils.setField(serviceTokenService, "delegatedTokenExpirationMs", 600000L);
    }

    private Claims decode(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).requireIssuer("core-k").build()
                .parseSignedClaims(token).getPayload();
    }

    @Test
    @DisplayName("delegated token contains sub, role, scenario_id, session_id, issuer, audience, and a future expiry")
    void mintDelegatedToken_containsExpectedClaims() {
        UUID userId = UUID.randomUUID();
        UUID scenarioId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        String token = serviceTokenService.mintDelegatedToken(userId, "EMPLOYEE", scenarioId, sessionId);
        Claims claims = decode(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("EMPLOYEE");
        assertThat(claims.get("scenario_id", String.class)).isEqualTo(scenarioId.toString());
        assertThat(claims.get("session_id", String.class)).isEqualTo(sessionId.toString());
        assertThat(claims.getIssuer()).isEqualTo("core-k");
        assertThat(claims.getAudience()).contains("ai-service-k");
        assertThat(claims.getExpiration()).isAfter(new java.util.Date());
    }

    @Test
    @DisplayName("two tokens minted for different sessions carry different session_id claims")
    void mintDelegatedToken_scopedToSession() {
        UUID userId = UUID.randomUUID();
        UUID scenarioId = UUID.randomUUID();

        String tokenA = serviceTokenService.mintDelegatedToken(userId, "EMPLOYEE", scenarioId, UUID.randomUUID());
        String tokenB = serviceTokenService.mintDelegatedToken(userId, "EMPLOYEE", scenarioId, UUID.randomUUID());

        assertThat(decode(tokenA).get("session_id")).isNotEqualTo(decode(tokenB).get("session_id"));
    }
}
