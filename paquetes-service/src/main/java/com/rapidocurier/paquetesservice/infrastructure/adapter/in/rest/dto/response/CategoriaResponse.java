package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.response;

import com.rapidocurier.paquetesservice.domain.model.Categoria;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Category response data")
public record CategoriaResponse(
    @Schema(description = "Category UUID")
    UUID id,
    @Schema(description = "Category name", example = "FRAGIL")
    String nombre,
    @Schema(description = "Category description", example = "Artículo frágil")
    String descripcion
) {
    public static CategoriaResponse fromDomain(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion());
    }
}
