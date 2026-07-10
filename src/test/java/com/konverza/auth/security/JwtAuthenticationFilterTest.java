package com.konverza.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private Claims claims;

    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private JwtAuthenticationFilter buildFilter() {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Test
    @DisplayName("valid bearer token sets the SecurityContext authentication")
    void doFilter_validToken_setsAuthentication() throws Exception {
        filter = buildFilter();
        UUID userId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(jwtService.parseAndValidate("valid.jwt.token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("role", String.class)).thenReturn("EMPLOYEE");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(userId.toString());
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_EMPLOYEE");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("invalid/expired token clears the SecurityContext and still continues the chain")
    void doFilter_invalidToken_clearsContextAndContinues() throws Exception {
        filter = buildFilter();
        when(request.getHeader("Authorization")).thenReturn("Bearer garbage.token");
        when(jwtService.parseAndValidate("garbage.token")).thenThrow(new ExpiredJwtException(null, null, "expired"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("missing Authorization header leaves the SecurityContext empty and continues the chain")
    void doFilter_noHeader_leavesContextEmpty() throws Exception {
        filter = buildFilter();
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("non-bearer Authorization header is ignored")
    void doFilter_nonBearerHeader_isIgnored() throws Exception {
        filter = buildFilter();
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }
}
