package com.rapidocurier.paquetesservice.infrastructure.config;

import com.rapidocurier.paquetesservice.domain.exception.ConflictException;
import com.rapidocurier.paquetesservice.domain.exception.ExternalServiceException;
import com.rapidocurier.paquetesservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.paquetesservice.domain.exception.TransicionInvalidaException;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void resourceNotFoundException_returns404() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Paquete no encontrado"));

        assertEquals(404, response.getStatusCode().value());
        assertFalse(response.getBody().success());
        assertEquals("Paquete no encontrado", response.getBody().message());
    }

    @Test
    void conflictException_returns409() {
        var response = handler.handleConflict(new ConflictException("El paquete debe tener al menos una categoría"));

        assertEquals(409, response.getStatusCode().value());
        assertFalse(response.getBody().success());
    }

    @Test
    void transicionInvalidaException_returns400() {
        var response = handler.handleTransicionInvalida(
            new TransicionInvalidaException(EstadoPaquete.REGISTRADO, EstadoPaquete.ENTREGADO));

        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().success());
    }

    @Test
    void externalServiceException_returns502() {
        var response = handler.handleExternalService(new ExternalServiceException("Error al consultar clients-service"));

        assertEquals(502, response.getStatusCode().value());
        assertFalse(response.getBody().success());
    }

    @Test
    void circuitBreakerException_returns502() {
        CircuitBreaker cb = CircuitBreaker.of("test", CircuitBreakerConfig.custom().build());
        CallNotPermittedException cbException = CallNotPermittedException.createCallNotPermittedException(cb);
        var response = handler.handleCircuitBreakerOpen(cbException);

        assertEquals(502, response.getStatusCode().value());
        assertFalse(response.getBody().success());
    }

    @Test
    void genericException_returns500() {
        var response = handler.handleGeneric(new RuntimeException("Unexpected error"));

        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().success());
    }
}
