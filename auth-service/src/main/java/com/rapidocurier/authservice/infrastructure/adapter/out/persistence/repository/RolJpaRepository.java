package com.rapidocurier.authservice.infrastructure.adapter.out.persistence.repository;

import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RolJpaRepository extends JpaRepository<RolEntity, UUID> {
    Optional<RolEntity> findByNombre(RolEntity.RolNombreEntity nombre);
}