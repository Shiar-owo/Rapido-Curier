package com.rapidocurier.paquetesservice.application.port.in;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequest(
    @NotBlank String nombre,
    String descripcion
) {}
