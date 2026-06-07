package com.rapidocurier.paquetesservice.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("RápidoCurier - Paquetes Service API")
                .description("Microservice for package registration, tracking and status management")
                .version("1.0.0")
                .contact(new Contact()
                    .name("RápidoCurier Team")
                    .email("dev@rapidocurier.com")))
            .schemaRequirement("Bearer",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer"));
    }
}
