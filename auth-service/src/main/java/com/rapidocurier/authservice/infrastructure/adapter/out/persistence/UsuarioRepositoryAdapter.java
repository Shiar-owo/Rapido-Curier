package com.rapidocurier.authservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.authservice.domain.model.Usuario;
import com.rapidocurier.authservice.domain.port.out.UsuarioRepositoryPort;
import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.entity.RolEntity;
import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.mapper.UsuarioMapper;
import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.repository.RolJpaRepository;
import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository jpaRepository;
    private final RolJpaRepository rolJpaRepository;
    private final UsuarioMapper mapper;

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existePorEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        var entity = mapper.toEntity(usuario);
        entity.setRoles(usuario.getRoles().stream()
                .map(rol -> rolJpaRepository.findByNombre(RolEntity.RolNombreEntity.valueOf(rol))
                        .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rol)))
                .collect(Collectors.toSet()));
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}