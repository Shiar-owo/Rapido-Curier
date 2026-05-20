package com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.mapper;

import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClienteMapperTest {

    private final ClienteMapperImpl mapper = new ClienteMapperImpl();

    @Test
    void toEntity_mapsAllFieldsFromDomain() {
        Cliente cliente = Cliente.create(
            "12345678",
            "Juan",
            "Perez",
            "Gomez",
            "juan@test.com"
        );

        ClienteEntity entity = mapper.toEntity(cliente);

        assertEquals(cliente.getDni(), entity.getDni());
        assertEquals(cliente.getNombre(), entity.getNombre());
        assertEquals(cliente.getApellidoPaterno(), entity.getApellidoPaterno());
        assertEquals(cliente.getApellidoMaterno(), entity.getApellidoMaterno());
        assertEquals(cliente.getEmail(), entity.getEmail());
    }

    @Test
    void toDomain_rehydratesFromEntityUsingRehydrateMethod() {
        UUID id = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updatedAt = OffsetDateTime.now();

        ClienteEntity entity = new ClienteEntity();
        entity.setId(id);
        entity.setDni("12345678");
        entity.setNombre("Juan");
        entity.setApellidoPaterno("Perez");
        entity.setApellidoMaterno("Gomez");
        entity.setEmail("juan@test.com");
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        Cliente cliente = mapper.toDomain(entity);

        assertEquals(id, cliente.getId());
        assertEquals("12345678", cliente.getDni());
        assertEquals("Juan", cliente.getNombre());
        assertEquals("Perez", cliente.getApellidoPaterno());
        assertEquals("Gomez", cliente.getApellidoMaterno());
        assertEquals("juan@test.com", cliente.getEmail());
        assertEquals(createdAt, cliente.getCreatedAt());
        assertEquals(updatedAt, cliente.getUpdatedAt());
    }

    @Test
    void toDomain_generatesCorrectNombreCompleto() {
        ClienteEntity entity = new ClienteEntity();
        entity.setId(UUID.randomUUID());
        entity.setDni("12345678");
        entity.setNombre("ROXANA KARINA");
        entity.setApellidoPaterno("DELGADO");
        entity.setApellidoMaterno("HUAMANI");
        entity.setEmail("roxana@test.com");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        Cliente cliente = mapper.toDomain(entity);

        assertEquals("ROXANA KARINA DELGADO HUAMANI", cliente.getNombreCompleto());
    }
}