package com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Client creation request")
public record ClienteRequest(

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
    @Schema(example = "12345678", description = "DNI — 8 digits")
    String dni,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Size(max = 100, message = "El email no debe exceder 100 caracteres")
    @Schema(example = "cliente@mail.com", description = "Client email")
    String email
) {}
