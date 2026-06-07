package com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.clientsservice.domain.exception.ConflictException;
import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.domain.port.out.ClienteRepositoryPort;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.mapper.ClienteMapper;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.repository.ClienteJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

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
        try {
            ClienteEntity entity = mapper.toEntity(cliente);
            return mapper.toDomain(repository.save(entity));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("El email o DNI ya está registrado");
        }
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
    public List<Cliente> buscarPorNombre(String nombre) {
        return repository
            .findByNombreContainingIgnoreCaseOrApellidoPaternoContainingIgnoreCaseOrApellidoMaternoContainingIgnoreCase(
                nombre, nombre, nombre)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void eliminar(UUID id) {
        repository.deleteById(id);
    }
}