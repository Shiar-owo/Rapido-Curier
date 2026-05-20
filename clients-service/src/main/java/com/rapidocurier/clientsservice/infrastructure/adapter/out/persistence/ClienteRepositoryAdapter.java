package com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.domain.port.out.ClienteRepositoryPort;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.mapper.ClienteMapper;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.repository.ClienteJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClienteRepositoryAdapter implements ClienteRepositoryPort {

    private final ClienteJpaRepository repository;
    private final ClienteMapper mapper;

    public ClienteRepositoryAdapter(ClienteJpaRepository repository, ClienteMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        ClienteEntity entity = mapper.toEntity(cliente);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Cliente> buscarPorDni(String dni) {
        return repository.findByDni(dni).map(mapper::toDomain);
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public List<Cliente> listarTodos() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void eliminar(UUID id) {
        repository.deleteById(id);
    }
}