package com.konverza.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates backend-to-backend calls from AI-Service-k that carry a
 * static service credential (X-Service-Key), as an alternative to the
 * user-facing Bearer JWT checked by JwtAuthenticationFilter. Runs first in
 * the chain; if the header is absent or doesn't match, it does nothing and
 * lets JwtAuthenticationFilter (or SecurityConfig's anyRequest().authenticated())
 * decide the outcome, so this never itself rejects a request.
 */
@Slf4j
@Component
public class ServiceKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String SERVICE_KEY_HEADER = "X-Service-Key";

    // No default: Spring fails to start if this is unset (see ServiceTokenService).
    @Value("${service.ai-service-api-key}")
    private String aiServiceApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String presented = request.getHeader(SERVICE_KEY_HEADER);
        if (presented != null && !presented.isBlank()) {
            if (presented.equals(aiServiceApiKey)) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        "ai-service-k", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("X-Service-Key invalido recibido en {}", request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }
}
