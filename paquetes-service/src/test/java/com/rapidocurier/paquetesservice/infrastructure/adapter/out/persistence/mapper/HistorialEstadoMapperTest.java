package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.HistorialEstadoEntity;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.PaqueteEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HistorialEstadoMapperTest {

    private final HistorialEstadoMapperImpl mapper = new HistorialEstadoMapperImpl();

    @Test
    void toEntity_mapsFieldsAndEstadoAsString() {
        UUID paqueteId = UUID.randomUUID();
        PaqueteEntity paqueteEntity = new PaqueteEntity();
        paqueteEntity.setId(paqueteId);

        EstadoHistorial domain = new EstadoHistorial(
            UUID.randomUUID(), paqueteId,
            EstadoPaquete.EN_ALMACEN,
            OffsetDateTime.now(), "operador1"
        );

        HistorialEstadoEntity entity = mapper.toEntity(domain, paqueteEntity);

        assertEquals(domain.getId(), entity.getId());
        assertEquals("EN_ALMACEN", entity.getEstado());
        assertEquals("operador1", entity.getUsuarioResponsable());
        assertEquals(paqueteId, entity.getPaquete().getId());
        assertNull(entity.getFechaCambio());
    }

    @Test
    void toDomain_rehydratesFromEntityWithPaqueteReference() {
        UUID paqueteId = UUID.randomUUID();
        UUID historialId = UUID.randomUUID();
        OffsetDateTime fechaCambio = OffsetDateTime.now();

        PaqueteEntity paqueteEntity = new PaqueteEntity();
        paqueteEntity.setId(paqueteId);

        HistorialEstadoEntity entity = new HistorialEstadoEntity();
        entity.setId(historialId);
        entity.setPaquete(paqueteEntity);
        entity.setEstado("ENTREGADO");
        entity.setFechaCambio(fechaCambio);
        entity.setUsuarioResponsable("repartidor1");

        EstadoHistorial domain = mapper.toDomain(entity);

        assertEquals(historialId, domain.getId());
        assertEquals(paqueteId, domain.getPaqueteId());
        assertEquals(EstadoPaquete.ENTREGADO, domain.getEstado());
        assertEquals(fechaCambio, domain.getFechaCambio());
        assertEquals("repartidor1", domain.getUsuarioResponsable());
    }

    @Test
    void roundTrip_toEntity_thenToDomain_preservesCoreData() {
        UUID paqueteId = UUID.randomUUID();
        EstadoHistorial original = new EstadoHistorial(
            UUID.randomUUID(), paqueteId,
            EstadoPaquete.EN_TRANSITO,
            OffsetDateTime.now(), "sistema"
        );

        PaqueteEntity paqueteEntity = new PaqueteEntity();
        paqueteEntity.setId(paqueteId);

        HistorialEstadoEntity entity = mapper.toEntity(original, paqueteEntity);

        EstadoHistorial result = mapper.toDomain(entity);

        assertEquals(original.getId(), result.getId());
        assertEquals(original.getPaqueteId(), result.getPaqueteId());
        assertEquals(original.getEstado(), result.getEstado());
        assertEquals(original.getUsuarioResponsable(), result.getUsuarioResponsable());
    }
}
