package com.gympro.chatbot.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
        title       = "GymPro – Chatbot Service API",
        version     = "1.0.0",
        description = "Conversational AI assistant for GymPro. All endpoints are public – no JWT required.",
        contact     = @Contact(name = "GymPro Team", email = "support@gympro.com")
    ),
    servers = @Server(url = "http://localhost:8088", description = "Local – Chatbot Service")
    // No @SecurityRequirement – chatbot is fully public
)
@SecurityScheme(
    name         = "bearerAuth",
    type         = SecuritySchemeType.HTTP,
    scheme       = "bearer",
    bearerFormat = "JWT",
    in           = SecuritySchemeIn.HEADER,
    description  = "Optional – chatbot endpoints do not enforce JWT."
)
@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi chatbotApi() {
        return GroupedOpenApi.builder()
                .group("chatbot-service")
                .pathsToMatch("/chatbot/**")
                .build();
    }
}
