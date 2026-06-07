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

    @Test
    void buscarPorNombre_porNombre_retornaCoincidencias() {
        repository.guardar(Cliente.create("44444444", "Carlos", "Lopez", "Diaz", "carlos@test.com"));
        repository.guardar(Cliente.create("77777777", "Maria", "Garcia", "Lopez", "maria2@test.com"));

        java.util.List<Cliente> resultados = repository.buscarPorNombre("Carlos");

        assertFalse(resultados.isEmpty());
        assertTrue(resultados.stream().anyMatch(c -> c.getNombre().equals("Carlos")));
    }

    @Test
    void buscarPorNombre_porApellidoPaterno_retornaCoincidencias() {
        repository.guardar(Cliente.create("66666666", "Ana", "Torres", "Ruiz", "ana@test.com"));

        java.util.List<Cliente> resultados = repository.buscarPorNombre("Torres");

        assertFalse(resultados.isEmpty());
        assertTrue(resultados.stream().anyMatch(c -> c.getApellidoPaterno().equals("Torres")));
    }

    @Test
    void buscarPorNombre_noCoinincide_retornaListaVacia() {
        java.util.List<Cliente> resultados = repository.buscarPorNombre("ZZZZZ");

        assertTrue(resultados.isEmpty());
    }
}