package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.repository;

import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.HistorialEstadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistorialEstadoJpaRepository extends JpaRepository<HistorialEstadoEntity, UUID> {

    List<HistorialEstadoEntity> findByPaqueteIdOrderByFechaCambioAsc(UUID paqueteId);
}
