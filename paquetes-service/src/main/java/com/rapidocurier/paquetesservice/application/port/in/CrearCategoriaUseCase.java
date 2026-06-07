package com.rapidocurier.paquetesservice.application.port.in;

import com.rapidocurier.paquetesservice.domain.model.Categoria;

public interface CrearCategoriaUseCase {
    Categoria crear(String nombre, String descripcion);
}
