package com.gympro.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
        title        = "GymPro – Auth Service API",
        version      = "1.0.0",
        description  = "Authentication & authorisation endpoints: register, login, OTP-based password reset.",
        contact      = @Contact(name = "GymPro Team", email = "support@gympro.com")
    ),
    servers = @Server(url = "http://localhost:8081", description = "Local – Auth Service"),
    // Apply JWT security globally; individual public ops override with security = {}
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name         = "bearerAuth",
    type         = SecuritySchemeType.HTTP,
    scheme       = "bearer",
    bearerFormat = "JWT",
    in           = SecuritySchemeIn.HEADER,
    description  = "Paste your JWT token (without 'Bearer ' prefix). Obtain one from POST /auth/login."
)
@Configuration
public class OpenApiConfig {

    /**
     * Expose all endpoints in a single Swagger group.
     * Auth endpoints are public; the JWT lock icon is shown but not enforced
     * because SecurityConfig already permits /auth/**.
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth-service")
                .pathsToMatch("/auth/**")
                .build();
    }
}
