package com.rapidocurier.clientsservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.clientsservice.ClientsServiceApplication;
import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.domain.port.out.ClienteRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ClientsServiceApplication.class)
@ActiveProfiles("test")
class ClienteRepositoryAdapterTest {

    @Autowired
    private ClienteRepositoryPort repository;

    @ServiceConnection
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Test
    void guardar_and_buscarPorId_returnsCliente() {
        Cliente cliente = Cliente.create(
            "12345678",
            "Juan",
            "Perez",
            "Gomez",
            "juan@test.com"
        );

        Cliente guardado = repository.guardar(cliente);

        Optional<Cliente> encontrado = repository.buscarPorId(guardado.getId());

        assertTrue(encontrado.isPresent());
        assertEquals("12345678", encontrado.get().getDni());
        assertEquals("Juan", encontrado.get().getNombre());
        assertEquals("juan@test.com", encontrado.get().getEmail());
    }

    @Test
    void buscarPorDni_returnsCliente() {
        Cliente cliente = Cliente.create(
            "87654321",
            "Maria",
            "Garcia",
            "Lopez",
            "maria@test.com"
        );

        repository.guardar(cliente);

        Optional<Cliente> encontrado = repository.buscarPorDni("87654321");

        assertTrue(encontrado.isPresent());
        assertEquals("Maria", encontrado.get().getNombre());
    }

    @Test
    void buscarPorEmail_returnsCliente() {
        Cliente cliente = Cliente.create(
            "55555555",
            "Pedro",
            "Rodriguez",
            "Martinez",
            "pedro@test.com"
        );

        repository.guardar(cliente);

        Optional<Cliente> encontrado = repository.buscarPorEmail("pedro@test.com");

        assertTrue(encontrado.isPresent());
        assertEquals("Pedro", encontrado.get().getNombre());
    }

    @Test
    void guardar_and_eliminar_buscarPorId_returnsEmpty() {
        Cliente cliente = Cliente.create(
            "11111111",
            "Test",
            "Test",
            "Test",
            "test@test.com"
        );

        Cliente guardado = repository.guardar(cliente);
        repository.eliminar(guardado.getId());

        Optional<Cliente> encontrado = repository.buscarPorId(guardado.getId());

        assertFalse(encontrado.isPresent());
    }

    @Test
    void listarTodos_returnsAllClientes() {
        int initialCount = repository.listarTodos().size();

        repository.guardar(Cliente.create("22222222", "A", "B", "C", "a@test.com"));
        repository.guardar(Cliente.create("33333333", "D", "E", "F", "d@test.com"));

        int finalCount = repository.listarTodos().size();

        assertEquals(initialCount + 2, finalCount);
    }
}