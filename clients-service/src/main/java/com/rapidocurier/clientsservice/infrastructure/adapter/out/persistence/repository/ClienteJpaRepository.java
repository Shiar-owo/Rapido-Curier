package com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.repository;

import com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, UUID> {

    Optional<ClienteEntity> findByDni(String dni);

    Optional<ClienteEntity> findByEmail(String email);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    List<ClienteEntity> findByNombreContainingIgnoreCaseOrApellidoPaternoContainingIgnoreCaseOrApellidoMaternoContainingIgnoreCase(
        String nombre, String apellidoPaterno, String apellidoMaterno);
}