package com.gympro.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — permits all requests for notification-service.
 *
 * The API Gateway handles JWT validation + forwards X-User-Email / X-User-Role headers.
 * No separate JWT validation needed here.
 *
 * SSE requires async support — Spring Security's CSRF must be disabled (stateless).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // SSE stream endpoint — must not be blocked
                .requestMatchers("/notify/inapp/*/stream").permitAll()
                // All other notification endpoints — Gateway already validated JWT
                .requestMatchers("/notify/**").permitAll()
                // Actuator health checks
                .requestMatchers("/actuator/**").permitAll()
                // Swagger UI
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }
}