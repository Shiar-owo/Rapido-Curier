package com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.dto.response;

import com.rapidocurier.clientsservice.domain.model.Cliente;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Client response data")
public record ClienteResponse(
    @Schema(description = "Client UUID")
    UUID id,
    @Schema(example = "12345678", description = "DNI")
    String dni,
    @Schema(example = "Juan", description = "First name")
    String nombre,
    @Schema(example = "Pérez", description = "First last name")
    String apellidoPaterno,
    @Schema(example = "García", description = "Second last name")
    String apellidoMaterno,
    @Schema(example = "juan@mail.com", description = "Email")
    String email,
    @Schema(description = "Creation timestamp")
    OffsetDateTime createdAt,
    @Schema(description = "Last update timestamp")
    OffsetDateTime updatedAt
) {
    public static ClienteResponse fromDomain(Cliente cliente) {
        return new ClienteResponse(
            cliente.getId(),
            cliente.getDni(),
            cliente.getNombre(),
            cliente.getApellidoPaterno(),
            cliente.getApellidoMaterno(),
            cliente.getEmail(),
            cliente.getCreatedAt(),
            cliente.getUpdatedAt()
        );
    }
}
