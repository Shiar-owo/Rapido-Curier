package com.rapidocurier.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("RapidoCurier API Gateway")
                .description("Central API Gateway — aggregates and routes requests to all backend microservices")
                .version("1.0.0"));
    }
}
