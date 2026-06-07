package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.repository;

import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, UUID> {
}
