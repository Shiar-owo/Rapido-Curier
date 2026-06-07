package com.rapidocurier.paquetesservice.application.service;

import com.rapidocurier.paquetesservice.application.port.in.ConsultarCategoriaUseCase;
import com.rapidocurier.paquetesservice.application.port.in.CrearCategoriaUseCase;
import com.rapidocurier.paquetesservice.domain.exception.ConflictException;
import com.rapidocurier.paquetesservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.port.out.CategoriaRepositoryPort;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoriaService implements ConsultarCategoriaUseCase, CrearCategoriaUseCase {

    private final CategoriaRepositoryPort categoriaRepo;

    public CategoriaService(CategoriaRepositoryPort categoriaRepo) {
        this.categoriaRepo = categoriaRepo;
    }

    @Override
    public Categoria crear(String nombre, String descripcion) {
        categoriaRepo.listarTodas().stream()
            .filter(c -> c.getNombre().equalsIgnoreCase(nombre))
            .findFirst()
            .ifPresent(c -> {
                throw new ConflictException("La categoría '" + nombre + "' ya existe");
            });

        Categoria categoria = new Categoria(null, nombre, descripcion);
        return categoriaRepo.guardar(categoria);
    }

    @Override
    public List<Categoria> listarTodas() {
        return categoriaRepo.listarTodas();
    }

    @Override
    public Categoria buscarPorId(UUID id) {
        return categoriaRepo.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }
}
