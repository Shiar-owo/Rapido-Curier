package com.rapidocurier.clientsservice.application.service;

import com.rapidocurier.clientsservice.domain.exception.ConflictException;
import com.rapidocurier.clientsservice.domain.exception.ExternalServiceException;
import com.rapidocurier.clientsservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.domain.model.ReniecDataClient;
import com.rapidocurier.clientsservice.domain.port.out.ClienteRepositoryPort;
import com.rapidocurier.clientsservice.domain.port.out.ReniecPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepositoryPort repositoryPort;

    @Mock
    private ReniecPort reniecPort;

    @InjectMocks
    private ClienteService service;

    @Test
    void registrar_happyPath_retornaCliente() {
        when(repositoryPort.buscarPorEmail("a@b.com")).thenReturn(Optional.empty());
        when(repositoryPort.buscarPorDni("12345678")).thenReturn(Optional.empty());
        when(reniecPort.obtenerDatos("12345678"))
            .thenReturn(new ReniecDataClient("ROXANA KARINA", "DELGADO", "HUAMANI", "DELGADO HUAMANI ROXANA KARINA", "12345678"));
        when(repositoryPort.guardar(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        Cliente resultado = service.registrar("12345678", "a@b.com");

        assertNotNull(resultado);
        assertEquals("ROXANA KARINA", resultado.getNombre());
        assertEquals("DELGADO", resultado.getApellidoPaterno());
        assertEquals("HUAMANI", resultado.getApellidoMaterno());
    }

    @Test
    void registrar_emailDuplicado_lanzaConflictException() {
        when(repositoryPort.buscarPorEmail("a@b.com"))
            .thenReturn(Optional.of(Cliente.create("12345678", "Nombre", "Paterno", "Materno", "a@b.com")));

        ConflictException ex = assertThrows(ConflictException.class,
            () -> service.registrar("12345678", "a@b.com"));

        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    void registrar_reniecFalla_lanzaExternalServiceException() {
        when(repositoryPort.buscarPorEmail(any())).thenReturn(Optional.empty());
        when(reniecPort.obtenerDatos(any()))
            .thenThrow(new ExternalServiceException("RENIEC caído"));

        assertThrows(ExternalServiceException.class,
            () -> service.registrar("12345678", "a@b.com"));
    }

    @Test
    void listar_repositorioVacio_retornaListaVacia() {
        when(repositoryPort.listarTodos()).thenReturn(List.of());

        assertTrue(service.listarTodos().isEmpty());
    }

    @Test
    void eliminar_clienteNoExiste_lanzaResourceNotFoundException() {
        when(repositoryPort.buscarPorId(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.eliminar(UUID.randomUUID()));
    }
}