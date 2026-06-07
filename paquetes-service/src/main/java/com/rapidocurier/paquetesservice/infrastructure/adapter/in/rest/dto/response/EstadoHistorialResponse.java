package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.response;

import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Status history entry response")
public record EstadoHistorialResponse(
    @Schema(description = "History entry UUID")
    UUID id,
    @Schema(description = "Package UUID")
    UUID paqueteId,
    @Schema(description = "Package status")
    EstadoPaquete estado,
    @Schema(description = "Change timestamp")
    OffsetDateTime fechaCambio,
    @Schema(description = "Responsible user", example = "operador1")
    String usuarioResponsable
) {
    public static EstadoHistorialResponse fromDomain(EstadoHistorial h) {
        return new EstadoHistorialResponse(
            h.getId(), h.getPaqueteId(), h.getEstado(),
            h.getFechaCambio(), h.getUsuarioResponsable()
        );
    }
}
