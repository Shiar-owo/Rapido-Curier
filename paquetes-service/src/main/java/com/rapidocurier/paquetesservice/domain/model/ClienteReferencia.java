package com.rapidocurier.paquetesservice.domain.model;

import java.util.UUID;

public record ClienteReferencia(
    UUID id,
    String dni,
    String nombre,
    String email
) {}
