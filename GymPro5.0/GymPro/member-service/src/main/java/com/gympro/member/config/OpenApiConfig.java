package com.gympro.member.config;

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
        title       = "GymPro – Member Service API",
        version     = "1.0.0",
        description = "CRUD operations for gym members. Most write/admin operations require the ADMIN role via the X-User-Role header.",
        contact     = @Contact(name = "GymPro Team", email = "support@gympro.com")
    ),
    servers  = @Server(url = "http://localhost:8082", description = "Local – Member Service"),
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
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder()
                .group("member-service")
                .pathsToMatch("/members/**")
                .build();
    }
}
