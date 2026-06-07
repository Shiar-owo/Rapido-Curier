package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.HistorialEstadoEntity;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.PaqueteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HistorialEstadoMapper {

    @Mapping(target = "id", source = "domain.id")
    @Mapping(target = "paquete", source = "paqueteEntity")
    @Mapping(target = "estado", expression = "java(domain.getEstado().name())")
    @Mapping(target = "fechaCambio", ignore = true)
    HistorialEstadoEntity toEntity(EstadoHistorial domain, PaqueteEntity paqueteEntity);

    default EstadoHistorial toDomain(HistorialEstadoEntity entity) {
        return new EstadoHistorial(
            entity.getId(),
            entity.getPaquete().getId(),
            EstadoPaquete.valueOf(entity.getEstado()),
            entity.getFechaCambio(),
            entity.getUsuarioResponsable()
        );
    }
}
