package com.rapidocurier.clientsservice.infrastructure.adapter.out.reniec.adapter;

import com.rapidocurier.clientsservice.domain.exception.ExternalServiceException;
import com.rapidocurier.clientsservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.clientsservice.domain.model.ReniecDataClient;
import com.rapidocurier.clientsservice.domain.port.out.ReniecPort;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.reniec.client.ReniecFeignClient;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReniecAdapter implements ReniecPort {

    private static final Logger log = LoggerFactory.getLogger(ReniecAdapter.class);

    private final ReniecFeignClient reniecFeignClient;

    @Value("${reniec.api.token}")
    private String token;

    public ReniecAdapter(ReniecFeignClient reniecFeignClient) {
        this.reniecFeignClient = reniecFeignClient;
    }

    @Override
    @CircuitBreaker(name = "reniec", fallbackMethod = "fallbackObtenerDatos")
    public ReniecDataClient obtenerDatos(String dni) {
        try {
            ReniecFeignClient.ReniecApiResponse response =
                reniecFeignClient.consultarDni("Bearer " + token, dni);
            return new ReniecDataClient(
                response.firstName(),
                response.firstLastName(),
                response.secondLastName(),
                response.fullName(),
                response.documentNumber()
            );
        } catch (FeignException.NotFound e) {
            log.warn("DNI {} no encontrado en RENIEC (404)", dni);
            throw new ResourceNotFoundException("DNI no encontrado en RENIEC: " + dni);
        } catch (FeignException e) {
            log.error("RENIEC respondió con error HTTP {}: {}", e.status(), e.getMessage());
            throw new ExternalServiceException(
                "RENIEC respondió con error HTTP " + e.status() + ": " + e.getMessage()
            );
        }
    }

    @SuppressWarnings("unused")
    private ReniecDataClient fallbackObtenerDatos(String dni, Exception ex) {
        if (ex instanceof ResourceNotFoundException) {
            throw (ResourceNotFoundException) ex;
        }
        throw new ExternalServiceException(
            "RENIEC no disponible (circuit breaker abierto): " + ex.getMessage()
        );
    }
}
