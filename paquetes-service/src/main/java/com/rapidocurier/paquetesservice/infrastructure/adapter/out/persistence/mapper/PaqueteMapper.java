package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.PaqueteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {CategoriaMapper.class})
public interface PaqueteMapper {

    @Mapping(target = "estadoActual", expression = "java(domain.getEstadoActual().name())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaqueteEntity toEntity(Paquete domain);

    default Paquete toDomain(PaqueteEntity entity) {
        return Paquete.rehydrate(
            entity.getId(),
            entity.getCodigoRastreo(),
            entity.getRemitenteId(),
            entity.getDestinatarioId(),
            entity.getPesoKg(),
            entity.getValorDeclarado(),
            entity.getSucursalOrigen(),
            entity.getSucursalDestino(),
            entity.getTarifa(),
            EstadoPaquete.valueOf(entity.getEstadoActual()),
            entity.getCategorias().stream()
                .map(cat -> new Categoria(cat.getId(), cat.getNombre(), cat.getDescripcion()))
                .collect(Collectors.toSet()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
