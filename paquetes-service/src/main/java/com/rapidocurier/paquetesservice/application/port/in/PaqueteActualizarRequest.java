package com.rapidocurier.paquetesservice.application.port.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record PaqueteActualizarRequest(
    @NotNull @Positive Double pesoKg,
    @NotNull @Positive Double valorDeclarado,
    String sucursalOrigen,
    String sucursalDestino
) {}
