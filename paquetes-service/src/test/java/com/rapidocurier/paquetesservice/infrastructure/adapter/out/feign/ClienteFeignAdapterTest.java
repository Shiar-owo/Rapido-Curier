package com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign;

import com.rapidocurier.paquetesservice.domain.exception.ExternalServiceException;
import com.rapidocurier.paquetesservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.paquetesservice.domain.model.ClienteReferencia;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.client.ClienteFeignClient;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.dto.FeignApiResponse;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.dto.ClienteResponse;

import feign.FeignException;
import feign.Request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteFeignAdapterTest {

    @Mock
    private ClienteFeignClient feignClient;

    private ClienteFeignAdapter adapter;

    private UUID clienteId;
    private ClienteResponse clienteResponse;

    @BeforeEach
    void setUp() {
        adapter = new ClienteFeignAdapter(feignClient);
        clienteId = UUID.randomUUID();
        clienteResponse = new ClienteResponse(
            clienteId, "12345678", "Juan", "Pérez", "García", "juan@test.com", null, null);
    }

    private Request dummyRequest(String url) {
        return Request.create(Request.HttpMethod.GET, url,
            Map.of(), (byte[]) null, null, null);
    }

    @Test
    void obtenerCliente_existe_retornaClienteReferencia() {
        FeignApiResponse<ClienteResponse> apiResponse = new FeignApiResponse<>(true, "OK", clienteResponse);
        when(feignClient.obtenerPorId(clienteId)).thenReturn(apiResponse);

        ClienteReferencia result = adapter.obtenerCliente(clienteId);

        assertAll(
            () -> assertEquals(clienteId, result.id()),
            () -> assertEquals("12345678", result.dni()),
            () -> assertEquals("Juan", result.nombre()),
            () -> assertEquals("juan@test.com", result.email())
        );
    }

    @Test
    void obtenerCliente_noExiste_lanzaResourceNotFoundException() {
        when(feignClient.obtenerPorId(clienteId))
            .thenThrow(new FeignException.NotFound("Not Found",
                dummyRequest("/api/v1/clientes/" + clienteId),
                new byte[0], Map.of()));

        assertThrows(ResourceNotFoundException.class, () -> adapter.obtenerCliente(clienteId));
    }

    @Test
    void obtenerCliente_responseNoExitoso_lanzaResourceNotFoundException() {
        FeignApiResponse<ClienteResponse> apiResponse = new FeignApiResponse<>(false, "No encontrado", null);
        when(feignClient.obtenerPorId(clienteId)).thenReturn(apiResponse);

        assertThrows(ResourceNotFoundException.class, () -> adapter.obtenerCliente(clienteId));
    }

    @Test
    void obtenerCliente_errorServidor_lanzaExternalServiceException() {
        when(feignClient.obtenerPorId(clienteId))
            .thenThrow(new FeignException.InternalServerError("Internal Server Error",
                dummyRequest("/api/v1/clientes/" + clienteId),
                new byte[0], Map.of()));

        assertThrows(ExternalServiceException.class, () -> adapter.obtenerCliente(clienteId));
    }

    @Test
    void buscarPorNombre_conResultados_retornaLista() {
        List<ClienteResponse> clientes = List.of(
            clienteResponse,
            new ClienteResponse(UUID.randomUUID(), "87654321", "Maria", "Lopez", "Diaz", "maria@test.com", null, null)
        );
        FeignApiResponse<List<ClienteResponse>> apiResponse = new FeignApiResponse<>(true, "OK", clientes);
        when(feignClient.buscarPorNombre("Juan")).thenReturn(apiResponse);

        List<ClienteReferencia> result = adapter.buscarPorNombre("Juan");

        assertEquals(2, result.size());
        assertEquals("Juan", result.get(0).nombre());
        assertEquals("Maria", result.get(1).nombre());
    }

    @Test
    void buscarPorNombre_sinResultados_retornaListaVacia() {
        FeignApiResponse<List<ClienteResponse>> apiResponse = new FeignApiResponse<>(true, "OK", List.of());
        when(feignClient.buscarPorNombre("Inexistente")).thenReturn(apiResponse);

        List<ClienteReferencia> result = adapter.buscarPorNombre("Inexistente");

        assertTrue(result.isEmpty());
    }

    @Test
    void buscarPorNombre_responseNoExitoso_retornaListaVacia() {
        FeignApiResponse<List<ClienteResponse>> apiResponse = new FeignApiResponse<>(false, "Error", null);
        when(feignClient.buscarPorNombre("Test")).thenReturn(apiResponse);

        List<ClienteReferencia> result = adapter.buscarPorNombre("Test");

        assertTrue(result.isEmpty());
    }

    @Test
    void buscarPorNombre_errorServidor_lanzaExternalServiceException() {
        when(feignClient.buscarPorNombre("Test"))
            .thenThrow(new FeignException.InternalServerError("Internal Server Error",
                dummyRequest("/api/v1/clientes/buscar?nombre=Test"),
                new byte[0], Map.of()));

        assertThrows(ExternalServiceException.class, () -> adapter.buscarPorNombre("Test"));
    }

    @Test
    void buscarPorEmail_existe_retornaClienteReferencia() {
        FeignApiResponse<ClienteResponse> apiResponse = new FeignApiResponse<>(true, "OK", clienteResponse);
        when(feignClient.buscarPorEmail("juan@test.com")).thenReturn(apiResponse);

        ClienteReferencia result = adapter.buscarPorEmail("juan@test.com");

        assertAll(
            () -> assertEquals(clienteId, result.id()),
            () -> assertEquals("12345678", result.dni()),
            () -> assertEquals("Juan", result.nombre()),
            () -> assertEquals("juan@test.com", result.email())
        );
    }

    @Test
    void buscarPorEmail_noExiste_lanzaResourceNotFoundException() {
        when(feignClient.buscarPorEmail("noexiste@test.com"))
            .thenThrow(new FeignException.NotFound("Not Found",
                dummyRequest("/api/v1/clientes/por-email?email=noexiste@test.com"),
                new byte[0], Map.of()));

        assertThrows(ResourceNotFoundException.class, () -> adapter.buscarPorEmail("noexiste@test.com"));
    }

    @Test
    void buscarPorEmail_responseNoExitoso_lanzaResourceNotFoundException() {
        FeignApiResponse<ClienteResponse> apiResponse = new FeignApiResponse<>(false, "No encontrado", null);
        when(feignClient.buscarPorEmail("test@test.com")).thenReturn(apiResponse);

        assertThrows(ResourceNotFoundException.class, () -> adapter.buscarPorEmail("test@test.com"));
    }

    @Test
    void buscarPorEmail_errorServidor_lanzaExternalServiceException() {
        when(feignClient.buscarPorEmail("test@test.com"))
            .thenThrow(new FeignException.InternalServerError("Internal Server Error",
                dummyRequest("/api/v1/clientes/por-email?email=test@test.com"),
                new byte[0], Map.of()));

        assertThrows(ExternalServiceException.class, () -> adapter.buscarPorEmail("test@test.com"));
    }
}
