package com.rapidocurier.paquetesservice.application.port.in;

import com.rapidocurier.paquetesservice.domain.model.Categoria;

import java.util.List;
import java.util.UUID;

public interface ConsultarCategoriaUseCase {
    List<Categoria> listarTodas();
    Categoria buscarPorId(UUID id);
}
