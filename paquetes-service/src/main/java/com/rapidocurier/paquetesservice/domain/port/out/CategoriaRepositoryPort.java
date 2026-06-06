package com.rapidocurier.paquetesservice.domain.port.out;

import com.rapidocurier.paquetesservice.domain.model.Categoria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepositoryPort {
    Categoria guardar(Categoria categoria);
    Optional<Categoria> buscarPorId(UUID id);
    List<Categoria> listarTodas();
}
