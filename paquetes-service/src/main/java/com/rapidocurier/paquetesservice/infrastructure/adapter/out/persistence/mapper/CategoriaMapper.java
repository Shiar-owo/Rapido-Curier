package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    CategoriaEntity toEntity(Categoria domain);

    default Categoria toDomain(CategoriaEntity entity) {
        return new Categoria(entity.getId(), entity.getNombre(), entity.getDescripcion());
    }
}
