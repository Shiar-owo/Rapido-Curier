package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.repository;

import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.PaqueteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaqueteJpaRepository extends JpaRepository<PaqueteEntity, UUID> {

    List<PaqueteEntity> findByCodigoRastreoContainingIgnoreCase(String texto);

    @Query("SELECT p FROM PaqueteEntity p WHERE p.estadoActual = :estado " +
           "AND (p.sucursalOrigen = :sucursal OR p.sucursalDestino = :sucursal)")
    List<PaqueteEntity> findBySucursalYEstado(
        @Param("sucursal") String sucursal, @Param("estado") String estado);

    @Query("SELECT p FROM PaqueteEntity p WHERE p.remitenteId IN :ids OR p.destinatarioId IN :ids")
    List<PaqueteEntity> findByRemitenteIdOrDestinatarioId(@Param("ids") List<UUID> ids);
}
