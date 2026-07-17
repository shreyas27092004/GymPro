package com.gympro.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for chatbot-service.
 *
 * The chatbot-service intentionally allows all /chatbot/** requests without authentication
 * because JWT validation is handled upstream by the API Gateway's JwtAuthFilter.
 * The Gateway strips or injects X-User-Email and X-User-Role headers after validation.
 *
 * The service itself does not need Spring Security for business logic — only for
 * disabling CSRF and permitting the Actuator and Swagger endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — this is a stateless REST API behind a gateway
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless — no HTTP sessions
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // All endpoints are publicly accessible:
            // JWT auth is enforced at the API Gateway layer, not here.
            .authorizeHttpRequests(auth -> auth
                // All chatbot endpoints (chat, health, conversation clearing)
                .requestMatchers("/chatbot/**").permitAll()

                // Spring Boot Actuator (health probes for Eureka + Docker)
                .requestMatchers(
                    "/actuator",
                    "/actuator/**"
                ).permitAll()

                // Swagger / OpenAPI docs
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**"
                ).permitAll()

                // Deny anything else (defensive default)
                .anyRequest().authenticated()
            );

        return http.build();
    }
}