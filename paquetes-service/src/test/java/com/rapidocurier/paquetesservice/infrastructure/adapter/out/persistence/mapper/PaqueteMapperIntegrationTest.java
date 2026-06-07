package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.PaqueteEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PaqueteMapperImpl.class, CategoriaMapperImpl.class})
class PaqueteMapperIntegrationTest {

    @Autowired
    private PaqueteMapper paqueteMapper;

    @Test
    void toEntity_delegatesCategoriaToCategoriaMapper() {
        UUID catId1 = UUID.randomUUID();
        UUID catId2 = UUID.randomUUID();
        Categoria cat1 = new Categoria(catId1, "FRAGIL", "Artículo frágil");
        Categoria cat2 = new Categoria(catId2, "DOCUMENTO", "Sobre documento");

        Paquete paquete = Paquete.create(
            UUID.randomUUID(), UUID.randomUUID(),
            5.0, 100.0,
            "LIMA", "AREQUIPA", 27.0,
            Set.of(cat1, cat2)
        );

        PaqueteEntity entity = paqueteMapper.toEntity(paquete);

        assertEquals(2, entity.getCategorias().size());
        assertTrue(entity.getCategorias().stream()
            .anyMatch(c -> c.getId().equals(catId1) && c.getNombre().equals("FRAGIL")));
        assertTrue(entity.getCategorias().stream()
            .anyMatch(c -> c.getId().equals(catId2) && c.getNombre().equals("DOCUMENTO")));
        assertEquals("REGISTRADO", entity.getEstadoActual());
    }

    @Test
    void roundTrip_fullCycle_withInjectedMappers() {
        UUID remitenteId = UUID.randomUUID();
        UUID destinatarioId = UUID.randomUUID();
        Categoria cat = new Categoria(UUID.randomUUID(), "ELECTRONICO", "Dispositivo");

        Paquete original = Paquete.create(
            remitenteId, destinatarioId,
            3.5, 250.0,
            "CUSCO", "AREQUIPA", 31.0,
            Set.of(cat)
        );

        PaqueteEntity entity = paqueteMapper.toEntity(original);
        Paquete result = paqueteMapper.toDomain(entity);

        assertAll(
            () -> assertEquals(original.getId(), result.getId()),
            () -> assertEquals(original.getCodigoRastreo(), result.getCodigoRastreo()),
            () -> assertEquals(original.getRemitenteId(), result.getRemitenteId()),
            () -> assertEquals(original.getDestinatarioId(), result.getDestinatarioId()),
            () -> assertEquals(original.getPesoKg(), result.getPesoKg()),
            () -> assertEquals(original.getValorDeclarado(), result.getValorDeclarado()),
            () -> assertEquals(original.getSucursalOrigen(), result.getSucursalOrigen()),
            () -> assertEquals(original.getSucursalDestino(), result.getSucursalDestino()),
            () -> assertEquals(original.getTarifa(), result.getTarifa()),
            () -> assertEquals(EstadoPaquete.REGISTRADO, result.getEstadoActual()),
            () -> assertEquals(1, result.getCategorias().size()),
            () -> assertEquals("ELECTRONICO", result.getCategorias().iterator().next().getNombre())
        );
    }

    @Test
    void toDomain_convertsEstadoActualStringToEnum() {
        UUID catId = UUID.randomUUID();
        Categoria cat = new Categoria(catId, "FRAGIL", "Frágil");

        Paquete original = Paquete.create(
            UUID.randomUUID(), UUID.randomUUID(),
            2.0, 50.0,
            "LIMA", "TRUJILLO", 13.0,
            Set.of(cat)
        );
        original.setEstadoActual(EstadoPaquete.EN_REPARTO);

        PaqueteEntity entity = paqueteMapper.toEntity(original);
        Paquete result = paqueteMapper.toDomain(entity);

        assertEquals(EstadoPaquete.EN_REPARTO, result.getEstadoActual());
        assertEquals(1, result.getCategorias().size());
        assertEquals(catId, result.getCategorias().iterator().next().getId());
        assertEquals("FRAGIL", result.getCategorias().iterator().next().getNombre());
    }
}
