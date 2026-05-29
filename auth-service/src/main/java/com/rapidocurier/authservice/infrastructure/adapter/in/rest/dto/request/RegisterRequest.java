package com.rapidocurier.authservice.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration data")
public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        @Schema(example = "Juan Pérez", description = "Full name")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        @Schema(example = "juan@example.com", description = "User email")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        @Schema(example = "securePass123", description = "Password (min 6 characters)")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        @Schema(example = "OPERADOR", description = "User role (ADMIN, OPERADOR)")
        String rol
) {}