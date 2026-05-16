package com.rapidocurier.authservice.infrastructure.adapter.out.persistence.repository;

import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {
    Optional<UsuarioEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}