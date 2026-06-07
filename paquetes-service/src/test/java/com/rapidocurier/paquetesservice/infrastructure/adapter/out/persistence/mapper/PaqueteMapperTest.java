package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.PaqueteEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaqueteMapperTest {

    private PaqueteMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new PaqueteMapperImpl();
        ReflectionTestUtils.setField(mapper, "categoriaMapper", new CategoriaMapperImpl());
    }

    @Test
    void toEntity_mapsAllFieldsAndEstadoAsString() {
        UUID catId = UUID.randomUUID();
        Categoria categoria = new Categoria(catId, "FRAGIL", "Frágil");
        Paquete paquete = Paquete.create(
            UUID.randomUUID(), UUID.randomUUID(),
            5.0, 100.0,
            "LIMA", "AREQUIPA", 27.0,
            Set.of(categoria)
        );

        PaqueteEntity entity = mapper.toEntity(paquete);

        assertEquals(paquete.getId(), entity.getId());
        assertEquals(paquete.getCodigoRastreo(), entity.getCodigoRastreo());
        assertEquals(paquete.getRemitenteId(), entity.getRemitenteId());
        assertEquals(paquete.getDestinatarioId(), entity.getDestinatarioId());
        assertEquals(paquete.getPesoKg(), entity.getPesoKg());
        assertEquals(paquete.getValorDeclarado(), entity.getValorDeclarado());
        assertEquals(paquete.getSucursalOrigen(), entity.getSucursalOrigen());
        assertEquals(paquete.getSucursalDestino(), entity.getSucursalDestino());
        assertEquals(paquete.getTarifa(), entity.getTarifa());
        assertEquals("REGISTRADO", entity.getEstadoActual());
        assertEquals(1, entity.getCategorias().size());
    }

    @Test
    void toEntity_categoriasMappedCorrectly() {
        UUID catId1 = UUID.randomUUID();
        UUID catId2 = UUID.randomUUID();
        Categoria cat1 = new Categoria(catId1, "FRAGIL", "Frágil");
        Categoria cat2 = new Categoria(catId2, "DOCUMENTO", "Documento");

        Paquete paquete = Paquete.create(
            UUID.randomUUID(), UUID.randomUUID(),
            3.0, 50.0,
            "CUSCO", "LIMA", 15.0,
            Set.of(cat1, cat2)
        );

        PaqueteEntity entity = mapper.toEntity(paquete);

        assertEquals(2, entity.getCategorias().size());
        assertTrue(entity.getCategorias().stream().anyMatch(c -> c.getId().equals(catId1)));
        assertTrue(entity.getCategorias().stream().anyMatch(c -> c.getId().equals(catId2)));
    }

    @Test
    void toDomain_rehydratesFromEntityUsingRehydrateMethod() {
        UUID id = UUID.randomUUID();
        UUID remitenteId = UUID.randomUUID();
        UUID destinatarioId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updatedAt = OffsetDateTime.now();

        CategoriaEntity catEntity = new CategoriaEntity();
        catEntity.setId(UUID.randomUUID());
        catEntity.setNombre("FRAGIL");
        catEntity.setDescripcion("Frágil");

        PaqueteEntity entity = new PaqueteEntity();
        entity.setId(id);
        entity.setCodigoRastreo("RC202606010001");
        entity.setRemitenteId(remitenteId);
        entity.setDestinatarioId(destinatarioId);
        entity.setPesoKg(5.0);
        entity.setValorDeclarado(100.0);
        entity.setSucursalOrigen("LIMA");
        entity.setSucursalDestino("AREQUIPA");
        entity.setTarifa(27.0);
        entity.setEstadoActual("EN_ALMACEN");
        entity.setCategorias(Set.of(catEntity));
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        Paquete paquete = mapper.toDomain(entity);

        assertEquals(id, paquete.getId());
        assertEquals("RC202606010001", paquete.getCodigoRastreo());
        assertEquals(remitenteId, paquete.getRemitenteId());
        assertEquals(destinatarioId, paquete.getDestinatarioId());
        assertEquals(5.0, paquete.getPesoKg());
        assertEquals(100.0, paquete.getValorDeclarado());
        assertEquals("LIMA", paquete.getSucursalOrigen());
        assertEquals("AREQUIPA", paquete.getSucursalDestino());
        assertEquals(27.0, paquete.getTarifa());
        assertEquals(EstadoPaquete.EN_ALMACEN, paquete.getEstadoActual());
        assertEquals(createdAt, paquete.getCreatedAt());
        assertEquals(updatedAt, paquete.getUpdatedAt());
    }

    @Test
    void toDomain_categoriasMappedToDomainSet() {
        CategoriaEntity catEntity = new CategoriaEntity();
        catEntity.setId(UUID.randomUUID());
        catEntity.setNombre("ELECTRONICO");
        catEntity.setDescripcion("Dispositivo electrónico");

        PaqueteEntity entity = new PaqueteEntity();
        entity.setId(UUID.randomUUID());
        entity.setCodigoRastreo("RC202606010002");
        entity.setRemitenteId(UUID.randomUUID());
        entity.setDestinatarioId(UUID.randomUUID());
        entity.setPesoKg(2.0);
        entity.setValorDeclarado(200.0);
        entity.setSucursalOrigen("AREQUIPA");
        entity.setSucursalDestino("CUSCO");
        entity.setTarifa(16.0);
        entity.setEstadoActual("EN_TRANSITO");
        entity.setCategorias(Set.of(catEntity));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        Paquete paquete = mapper.toDomain(entity);

        assertEquals(1, paquete.getCategorias().size());
        Categoria cat = paquete.getCategorias().iterator().next();
        assertEquals("ELECTRONICO", cat.getNombre());
        assertEquals("Dispositivo electrónico", cat.getDescripcion());
    }

    @Test
    void roundTrip_toEntity_thenToDomain_preservesCoreData() {
        UUID remitenteId = UUID.randomUUID();
        UUID destinatarioId = UUID.randomUUID();
        Categoria categoria = new Categoria(UUID.randomUUID(), "FRAGIL", "Frágil");

        Paquete original = Paquete.create(
            remitenteId, destinatarioId,
            10.0, 500.0,
            "LIMA", "CUSCO", 105.0,
            Set.of(categoria)
        );

        PaqueteEntity entity = mapper.toEntity(original);
        Paquete result = mapper.toDomain(entity);

        assertEquals(original.getId(), result.getId());
        assertEquals(original.getCodigoRastreo(), result.getCodigoRastreo());
        assertEquals(original.getRemitenteId(), result.getRemitenteId());
        assertEquals(original.getDestinatarioId(), result.getDestinatarioId());
        assertEquals(original.getPesoKg(), result.getPesoKg());
        assertEquals(original.getValorDeclarado(), result.getValorDeclarado());
        assertEquals(original.getSucursalOrigen(), result.getSucursalOrigen());
        assertEquals(original.getSucursalDestino(), result.getSucursalDestino());
        assertEquals(original.getTarifa(), result.getTarifa());
        assertEquals(original.getEstadoActual(), result.getEstadoActual());
    }
}
