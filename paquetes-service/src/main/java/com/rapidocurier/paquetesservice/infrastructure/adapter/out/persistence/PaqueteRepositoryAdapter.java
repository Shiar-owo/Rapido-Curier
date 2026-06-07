package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.domain.port.out.PaqueteRepositoryPort;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.PaqueteEntity;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper.PaqueteMapper;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.repository.PaqueteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaqueteRepositoryAdapter implements PaqueteRepositoryPort {

    private final PaqueteJpaRepository repository;
    private final PaqueteMapper mapper;

    @Override
    public Paquete guardar(Paquete paquete) {
        PaqueteEntity entity = mapper.toEntity(paquete);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Paquete> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Paquete> buscarPorCodigoRastreo(String texto) {
        return repository.findByCodigoRastreoContainingIgnoreCase(texto)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Paquete> buscarPorSucursalYEstado(String sucursal, EstadoPaquete estado) {
        return repository.findBySucursalYEstado(sucursal, estado.name())
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Paquete> buscarPorRemitenteIdOrDestinatarioId(Set<UUID> clienteIds) {
        return repository.findByRemitenteIdOrDestinatarioId(List.copyOf(clienteIds))
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void eliminar(UUID id) {
        repository.deleteById(id);
    }
}
