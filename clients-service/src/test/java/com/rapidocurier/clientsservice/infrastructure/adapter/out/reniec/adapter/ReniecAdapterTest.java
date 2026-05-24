package com.rapidocurier.clientsservice.infrastructure.adapter.out.reniec.adapter;

import com.rapidocurier.clientsservice.domain.exception.ExternalServiceException;
import com.rapidocurier.clientsservice.domain.model.ReniecDataClient;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.reniec.client.ReniecFeignClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReniecAdapterTest {

    @Mock
    private ReniecFeignClient reniecFeignClient;

    private ReniecAdapter reniecAdapter;

    @BeforeEach
    void setUp() {
        reniecAdapter = new ReniecAdapter(reniecFeignClient);
        ReflectionTestUtils.setField(reniecAdapter, "token", "test-token-123");
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
    void fallback_cuandoReniecFalla_lanzaExternalServiceException() throws Exception {
        var method = ReniecAdapter.class.getDeclaredMethod(
            "fallbackObtenerDatos", String.class, Exception.class);
        method.setAccessible(true);

        var invocationTarget = assertThrows(java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(reniecAdapter, "46027897", new RuntimeException("timeout")));

        assertInstanceOf(ExternalServiceException.class, invocationTarget.getCause());
        assertTrue(invocationTarget.getCause().getMessage().contains("RENIEC no disponible"));
    }
}
