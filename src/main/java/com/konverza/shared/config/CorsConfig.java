package com.konverza.shared.config;

import com.konverza.auth.controller.AuthController;
import com.konverza.auth.security.SecurityConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Exposed as a CorsConfigurationSource (not a WebMvcConfigurer) so Spring
     * Security's .cors(Customizer.withDefaults()) in SecurityConfig picks it up
     * and applies it to preflight requests before the auth filter runs.
     * allowCredentials(true) is required for the refresh-token cookie set by
     * AuthController to be sent/received cross-origin from Web-k.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
