package com.rapidocurier.authservice.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials")
public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        @Schema(example = "user@example.com", description = "User email")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Schema(example = "mypassword", description = "User password")
        String password
) {}