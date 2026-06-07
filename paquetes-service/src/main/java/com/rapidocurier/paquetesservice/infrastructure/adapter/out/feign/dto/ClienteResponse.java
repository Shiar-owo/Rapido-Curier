package com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.dto;

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
) {}
