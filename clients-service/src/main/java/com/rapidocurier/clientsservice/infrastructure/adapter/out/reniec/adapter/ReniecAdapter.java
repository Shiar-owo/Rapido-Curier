package com.rapidocurier.clientsservice.infrastructure.adapter.out.reniec.adapter;

import com.rapidocurier.clientsservice.domain.exception.ExternalServiceException;
import com.rapidocurier.clientsservice.domain.model.ReniecDataClient;
import com.rapidocurier.clientsservice.domain.port.out.ReniecPort;
import com.rapidocurier.clientsservice.infrastructure.adapter.out.reniec.client.ReniecFeignClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReniecAdapter implements ReniecPort {

    private final ReniecFeignClient reniecFeignClient;

    @Value("${reniec.api.token}")
    private String token;

    public ReniecAdapter(ReniecFeignClient reniecFeignClient) {
        this.reniecFeignClient = reniecFeignClient;
    }

    @Override
    @CircuitBreaker(name = "reniec", fallbackMethod = "fallbackObtenerDatos")
    public ReniecDataClient obtenerDatos(String dni) {
        ReniecFeignClient.ReniecApiResponse response =
            reniecFeignClient.consultarDni("Bearer " + token, dni);
        return new ReniecDataClient(
            response.firstName(),
            response.firstLastName(),
            response.secondLastName(),
            response.fullName(),
            response.documentNumber()
        );
    }

    @SuppressWarnings("unused")
    private ReniecDataClient fallbackObtenerDatos(String dni, Exception ex) {
        throw new ExternalServiceException(
            "RENIEC no disponible (circuit breaker abierto): " + ex.getMessage()
        );
    }
}
