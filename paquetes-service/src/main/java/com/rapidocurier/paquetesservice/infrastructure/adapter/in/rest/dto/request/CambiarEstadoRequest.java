package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.request;

import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change package status")
public record CambiarEstadoRequest(
    @Schema(description = "New package status")
    @NotNull EstadoPaquete nuevoEstado,

    @Schema(description = "User responsible for the change", example = "operador1")
    @NotNull String usuarioResponsable
) {}
