package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.port.out.CategoriaRepositoryPort;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper.CategoriaMapper;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.repository.CategoriaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoriaRepositoryAdapter implements CategoriaRepositoryPort {

    private final CategoriaJpaRepository repository;
    private final CategoriaMapper mapper;

    @Override
    public Categoria guardar(Categoria categoria) {
        CategoriaEntity entity = mapper.toEntity(categoria);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Categoria> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Categoria> listarTodas() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
}
