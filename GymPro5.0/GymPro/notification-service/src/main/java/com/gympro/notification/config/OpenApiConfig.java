package com.gympro.notification.config;

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
        title       = "GymPro – Notification Service API",
        version     = "1.0.0",
        description = "Internal email notification endpoints. Called by other services via OpenFeign; not exposed directly to end users.",
        contact     = @Contact(name = "GymPro Team", email = "support@gympro.com")
    ),
    servers  = @Server(url = "http://localhost:8086", description = "Local – Notification Service"),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name         = "bearerAuth",
    type         = SecuritySchemeType.HTTP,
    scheme       = "bearer",
    bearerFormat = "JWT",
    in           = SecuritySchemeIn.HEADER,
    description  = "Internal service – JWT not normally required. Present for completeness."
)
@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("notification-service")
                .pathsToMatch("/notify/**")
                .build();
    }
}
