package com.gympro.trainer.config;

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
        title       = "GymPro – Trainer Service API",
        version     = "1.0.0",
        description = "Trainer profiles and schedule/slot management.",
        contact     = @Contact(name = "GymPro Team", email = "support@gympro.com")
    ),
    servers  = @Server(url = "http://localhost:8083", description = "Local – Trainer Service"),
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

    @Bean
    public GroupedOpenApi trainerApi() {
        return GroupedOpenApi.builder()
                .group("trainer-service")
                .pathsToMatch("/trainers/**")
                .build();
    }
}
