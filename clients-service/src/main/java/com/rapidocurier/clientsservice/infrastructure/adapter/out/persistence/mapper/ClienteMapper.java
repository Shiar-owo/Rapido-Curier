package com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ClienteEntity toEntity(Cliente domain);

    default Cliente toDomain(ClienteEntity entity) {
        return Cliente.rehydrate(
            entity.getId(),
            entity.getDni(),
            entity.getNombre(),
            entity.getApellidoPaterno(),
            entity.getApellidoMaterno(),
            entity.getEmail(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}