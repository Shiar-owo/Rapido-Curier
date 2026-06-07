package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Request to register a new package")
public record PaqueteRegistrarRequest(
    @Schema(description = "UUID of the sender", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotNull UUID remitenteId,

    @Schema(description = "UUID of the recipient", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    @NotNull UUID destinatarioId,

    @Schema(description = "Package weight in kg", example = "5.5")
    @NotNull @Positive Double pesoKg,

    @Schema(description = "Declared value", example = "150.0")
    @NotNull @Positive Double valorDeclarado,

    @Schema(description = "Origin branch", example = "LIMA")
    @NotBlank String sucursalOrigen,

    @Schema(description = "Destination branch", example = "AREQUIPA")
    @NotBlank String sucursalDestino,

    @Schema(description = "Category UUIDs", example = "[\"c1d2e3f4-a5b6-7890-cdef-123456789012\"]")
    @NotEmpty Set<UUID> categoriaIds
) {}
