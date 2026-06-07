package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.port.out.HistorialRepositoryPort;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.HistorialEstadoEntity;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.PaqueteEntity;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper.HistorialEstadoMapper;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.repository.HistorialEstadoJpaRepository;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.repository.PaqueteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HistorialRepositoryAdapter implements HistorialRepositoryPort {

    private final HistorialEstadoJpaRepository repository;
    private final PaqueteJpaRepository paqueteRepository;
    private final HistorialEstadoMapper mapper;

    @Override
    public void guardar(EstadoHistorial historial) {
        PaqueteEntity paqueteEntity = paqueteRepository.findById(historial.getPaqueteId())
            .orElseThrow(() -> new RuntimeException("Paquete no encontrado: " + historial.getPaqueteId()));

        HistorialEstadoEntity entity = mapper.toEntity(historial, paqueteEntity);
        repository.save(entity);
    }

    @Override
    public List<EstadoHistorial> obtenerPorPaqueteId(UUID paqueteId) {
        return repository.findByPaqueteIdOrderByFechaCambioAsc(paqueteId)
            .stream().map(mapper::toDomain).toList();
    }
}
