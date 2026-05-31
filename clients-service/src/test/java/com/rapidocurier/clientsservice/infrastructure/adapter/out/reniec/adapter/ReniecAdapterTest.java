package com.rapidocurier.clientsservice.infrastructure.adapter.out.reniec.adapter;

import com.rapidocurier.clientsservice.domain.exception.ExternalServiceException;
import com.rapidocurier.clientsservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.clientsservice.domain.model.ReniecDataClient;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.reniec.client.ReniecFeignClient;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReniecAdapterTest {

    @Mock
    private ReniecFeignClient reniecFeignClient;

    private ReniecAdapter reniecAdapter;

    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        reniecAdapter = new ReniecAdapter(reniecFeignClient);
        ReflectionTestUtils.setField(reniecAdapter, "token", "test-token-123");
        dummyRequest = Request.create(Request.HttpMethod.GET, "http://reniec/api",
            Collections.emptyMap(), null, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void obtenerDatos_happyPath_retornaReniecDataClient() {
        var apiResponse = new ReniecFeignClient.ReniecApiResponse(
            "ROXANA KARINA", "DELGADO", "HUAMANI",
            "DELGADO HUAMANI ROXANA KARINA", "46027897"
        );
        when(reniecFeignClient.consultarDni(anyString(), anyString())).thenReturn(apiResponse);

        ReniecDataClient result = reniecAdapter.obtenerDatos("46027897");

        assertAll(
            () -> assertEquals("ROXANA KARINA", result.firstName()),
            () -> assertEquals("DELGADO", result.firstLastName()),
            () -> assertEquals("HUAMANI", result.secondLastName()),
            () -> assertEquals("DELGADO HUAMANI ROXANA KARINA", result.fullName()),
            () -> assertEquals("46027897", result.documentNumber())
        );
    }

    @Test
    void obtenerDatos_enviaBearerToken() {
        var apiResponse = new ReniecFeignClient.ReniecApiResponse(
            "NOMBRE", "PATERNO", "MATERNO", "NOMBRE COMPLETO", "12345678"
        );
        when(reniecFeignClient.consultarDni("Bearer test-token-123", "12345678"))
            .thenReturn(apiResponse);

        ReniecDataClient result = reniecAdapter.obtenerDatos("12345678");

        assertEquals("12345678", result.documentNumber());
    }

    @Test
    void obtenerDatos_dniNoEncontrado_lanzaResourceNotFoundException() {
        when(reniecFeignClient.consultarDni(anyString(), anyString()))
            .thenThrow(new FeignException.NotFound("Not Found",
                dummyRequest, null, Collections.emptyMap()));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
            () -> reniecAdapter.obtenerDatos("99999999"));

        assertTrue(ex.getMessage().contains("99999999"));
    }

    @Test
    void obtenerDatos_errorInterno_lanzaExternalServiceException() {
        when(reniecFeignClient.consultarDni(anyString(), anyString()))
            .thenThrow(new FeignException.InternalServerError("Internal Server Error",
                dummyRequest, null, Collections.emptyMap()));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
            () -> reniecAdapter.obtenerDatos("12345678"));

        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void obtenerDatos_serviceUnavailable_lanzaExternalServiceException() {
        when(reniecFeignClient.consultarDni(anyString(), anyString()))
            .thenThrow(new FeignException.ServiceUnavailable("Service Unavailable",
                dummyRequest, null, Collections.emptyMap()));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
            () -> reniecAdapter.obtenerDatos("12345678"));

        assertTrue(ex.getMessage().contains("503"));
    }

    @Test
    void obtenerDatos_badGateway_lanzaExternalServiceException() {
        when(reniecFeignClient.consultarDni(anyString(), anyString()))
            .thenThrow(new FeignException.BadGateway("Bad Gateway",
                dummyRequest, null, Collections.emptyMap()));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
            () -> reniecAdapter.obtenerDatos("12345678"));

        assertTrue(ex.getMessage().contains("502"));
    }

    @Test
    void fallback_cuandoReniecFalla_lanzaExternalServiceException() throws Exception {
        var method = ReniecAdapter.class.getDeclaredMethod(
            "fallbackObtenerDatos", String.class, Exception.class);
        method.setAccessible(true);

        var invocationTarget = assertThrows(java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(reniecAdapter, "46027897", new RuntimeException("timeout")));

        assertInstanceOf(ExternalServiceException.class, invocationTarget.getCause());
        assertTrue(invocationTarget.getCause().getMessage().contains("RENIEC no disponible"));
    }

    @Test
    void fallback_cuandoReniecDevuelve404_repitaResourceNotFoundException() throws Exception {
        var method = ReniecAdapter.class.getDeclaredMethod(
            "fallbackObtenerDatos", String.class, Exception.class);
        method.setAccessible(true);

        var invocationTarget = assertThrows(java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(reniecAdapter, "99999999",
                new ResourceNotFoundException("DNI no encontrado en RENIEC: 99999999")));

        assertInstanceOf(ResourceNotFoundException.class, invocationTarget.getCause());
    }
}
