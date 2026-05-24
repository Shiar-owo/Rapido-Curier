package com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.dto.response;

import com.rapidocurier.clientsservice.domain.model.Cliente;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteResponse(
    UUID id,
    String dni,
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno,
    String email,
    OffsetDateTime createdAt,
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
