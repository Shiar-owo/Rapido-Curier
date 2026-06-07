package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoriaMapperTest {

    private final CategoriaMapperImpl mapper = new CategoriaMapperImpl();

    @Test
    void toEntity_mapsAllFields() {
        UUID id = UUID.randomUUID();
        Categoria domain = new Categoria(id, "FRAGIL", "Artículo frágil");

        CategoriaEntity entity = mapper.toEntity(domain);

        assertEquals(id, entity.getId());
        assertEquals("FRAGIL", entity.getNombre());
        assertEquals("Artículo frágil", entity.getDescripcion());
    }

    @Test
    void toDomain_mapsAllFields() {
        CategoriaEntity entity = new CategoriaEntity();
        entity.setId(UUID.randomUUID());
        entity.setNombre("DOCUMENTO");
        entity.setDescripcion("Sobre documento");

        Categoria domain = mapper.toDomain(entity);

        assertEquals(entity.getId(), domain.getId());
        assertEquals("DOCUMENTO", domain.getNombre());
        assertEquals("Sobre documento", domain.getDescripcion());
    }

    @Test
    void roundTrip_toEntity_thenToDomain_preservesData() {
        Categoria original = new Categoria(UUID.randomUUID(), "ELECTRONICO", "Dispositivo electrónico");

        CategoriaEntity entity = mapper.toEntity(original);
        Categoria result = mapper.toDomain(entity);

        assertEquals(original.getId(), result.getId());
        assertEquals(original.getNombre(), result.getNombre());
        assertEquals(original.getDescripcion(), result.getDescripcion());
    }
}
