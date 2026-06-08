package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.response;

import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "Package response data")
public record PaqueteResponse(
    @Schema(description = "Package UUID")
    UUID id,
    @Schema(description = "Tracking code", example = "RC2026061234")
    String codigoRastreo,
    @Schema(description = "Sender UUID")
    UUID remitenteId,
    @Schema(description = "Sender full name from RENIEC (via clients-service)")
    String remitenteNombre,
    @Schema(description = "Recipient UUID")
    UUID destinatarioId,
    @Schema(description = "Recipient full name from RENIEC (via clients-service)")
    String destinatarioNombre,
    @Schema(description = "Weight in kg", example = "5.5")
    Double pesoKg,
    @Schema(description = "Declared value", example = "150.0")
    Double valorDeclarado,
    @Schema(description = "Origin branch", example = "LIMA")
    String sucursalOrigen,
    @Schema(description = "Destination branch", example = "AREQUIPA")
    String sucursalDestino,
    @Schema(description = "Tariff", example = "27.0")
    Double tarifa,
    @Schema(description = "Current status")
    EstadoPaquete estadoActual,
    @Schema(description = "Package categories")
    Set<CategoriaResponse> categorias,
    @Schema(description = "Creation timestamp")
    OffsetDateTime createdAt,
    @Schema(description = "Last update timestamp")
    OffsetDateTime updatedAt
) {
    public static PaqueteResponse fromDomain(Paquete paquete, String remitenteNombre, String destinatarioNombre) {
        Set<CategoriaResponse> cats = paquete.getCategorias().stream()
            .map(c -> new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion()))
            .collect(Collectors.toSet());
        return new PaqueteResponse(
            paquete.getId(),
            paquete.getCodigoRastreo(),
            paquete.getRemitenteId(),
            remitenteNombre,
            paquete.getDestinatarioId(),
            destinatarioNombre,
            paquete.getPesoKg(),
            paquete.getValorDeclarado(),
            paquete.getSucursalOrigen(),
            paquete.getSucursalDestino(),
            paquete.getTarifa(),
            paquete.getEstadoActual(),
            cats,
            paquete.getCreatedAt(),
            paquete.getUpdatedAt()
        );
    }
}
