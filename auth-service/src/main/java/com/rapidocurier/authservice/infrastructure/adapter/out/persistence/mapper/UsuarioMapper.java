package com.rapidocurier.authservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.authservice.domain.model.Usuario;
import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    UsuarioEntity toEntity(Usuario usuario);

    default Usuario toDomain(UsuarioEntity entity) {
        if (entity == null) return null;
        return new Usuario(
                entity.getId(),
                entity.getNombre(),
                entity.getPassword(),
                entity.getEmail(),
                entity.getRoles().stream()
                        .map(r -> r.getNombre().name())
                        .collect(java.util.stream.Collectors.toSet())
        );
    }
}