package com.gympro.plan.config;

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
        title       = "GymPro – Plan Service API",
        version     = "1.0.0",
        description = "Membership plan management and subscriptions. Admins manage plans; members subscribe.",
        contact     = @Contact(name = "GymPro Team", email = "support@gympro.com")
    ),
    servers  = @Server(url = "http://localhost:8084", description = "Local – Plan Service"),
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
    public GroupedOpenApi planApi() {
        return GroupedOpenApi.builder()
                .group("plan-service")
                .pathsToMatch("/plans/**")
                .build();
    }
}
