package com.gympro.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global CORS configuration for the API Gateway.
 *
 * Allows the React frontend (Vite dev server or production build) to call the gateway.
 * In production, replace localhost origins with your actual domain.
 *
 * Spring Cloud Gateway handles CORS at the reactive layer; this must be a CorsWebFilter,
 * NOT a WebMvcConfigurer (which is for Spring MVC, not WebFlux).
 */
@Configuration
public class CorsConfig {

    /**
     * Comma-separated list of allowed origins — configurable via environment variable.
     * Default: local development origins for Vite and Create React App.
     */
    @Value("${cors.allowed.origins:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}")
    private String allowedOriginsConfig;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated origins from config
        List<String> origins = List.of(allowedOriginsConfig.split(","));
        config.setAllowedOrigins(origins);

        // Allow all standard HTTP methods including OPTIONS (preflight)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers needed by the frontend
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-User-Role",
            "X-User-Email",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // Expose these headers so the React client can read them from responses
        config.setExposedHeaders(List.of(
            "Authorization",
            "X-User-Role",
            "X-User-Email"
        ));

        // Allow cookies / credentials (required if you use auth cookies; safe with specific origins)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour (reduces OPTIONS round-trips)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}