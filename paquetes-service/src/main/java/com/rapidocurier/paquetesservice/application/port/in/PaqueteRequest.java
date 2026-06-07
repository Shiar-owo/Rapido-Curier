package com.rapidocurier.paquetesservice.application.port.in;

import java.util.Set;
import java.util.UUID;

public record PaqueteRequest(
    UUID remitenteId,
    UUID destinatarioId,
    Double pesoKg,
    Double valorDeclarado,
    String sucursalOrigen,
    String sucursalDestino,
    Set<UUID> categoriaIds
) {}
