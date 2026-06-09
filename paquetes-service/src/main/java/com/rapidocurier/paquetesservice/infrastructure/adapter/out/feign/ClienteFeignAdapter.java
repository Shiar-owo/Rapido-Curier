package com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign;

import com.rapidocurier.paquetesservice.domain.exception.ExternalServiceException;
import com.rapidocurier.paquetesservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.paquetesservice.domain.model.ClienteReferencia;
import com.rapidocurier.paquetesservice.domain.port.out.ClienteFeignPort;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.client.ClienteFeignClient;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.dto.FeignApiResponse;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.dto.ClienteResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import feign.FeignException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ClienteFeignAdapter implements ClienteFeignPort {

    private static final Logger log = LoggerFactory.getLogger(ClienteFeignAdapter.class);
    private static final String CLIENTS_SERVICE = "clients-service";

    private final ClienteFeignClient feignClient;

    public ClienteFeignAdapter(ClienteFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    @Retry(name = CLIENTS_SERVICE)
    @CircuitBreaker(name = CLIENTS_SERVICE, fallbackMethod = "obtenerClienteFallback")
    public ClienteReferencia obtenerCliente(UUID id) {
        try {
            FeignApiResponse<ClienteResponse> response = feignClient.obtenerPorId(id);

            if (!response.success() || response.data() == null) {
                throw new ResourceNotFoundException("Cliente no encontrado: " + id);
            }

            ClienteResponse cr = response.data();
            String fullName = (cr.nombre() + " " + cr.apellidoPaterno() + " " + cr.apellidoMaterno()).trim();
            return new ClienteReferencia(cr.id(), cr.dni(), cr.nombre(), fullName, cr.email());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Cliente no encontrado: " + id);
        } catch (FeignException e) {
            log.error("Error calling clients-service for obtenerCliente: {}", e.getMessage());
            throw new ExternalServiceException("Error al consultar clients-service: " + e.getMessage());
        }
    }

    @Override
    @Retry(name = CLIENTS_SERVICE)
    @CircuitBreaker(name = CLIENTS_SERVICE, fallbackMethod = "buscarPorNombreFallback")
    public List<ClienteReferencia> buscarPorNombre(String nombre) {
        try {
            FeignApiResponse<List<ClienteResponse>> response = feignClient.buscarPorNombre(nombre);

            if (!response.success() || response.data() == null) {
                return List.of();
            }

            return response.data().stream()
                .map(cr -> {
                    String fullName = (cr.nombre() + " " + cr.apellidoPaterno() + " " + cr.apellidoMaterno()).trim();
                    return new ClienteReferencia(cr.id(), cr.dni(), cr.nombre(), fullName, cr.email());
                })
                .toList();
        } catch (FeignException e) {
            log.error("Error calling clients-service for buscarPorNombre: {}", e.getMessage());
            throw new ExternalServiceException("Error al consultar clients-service: " + e.getMessage());
        }
    }

    @Override
    @Retry(name = CLIENTS_SERVICE)
    @CircuitBreaker(name = CLIENTS_SERVICE, fallbackMethod = "buscarPorEmailFallback")
    public ClienteReferencia buscarPorEmail(String email) {
        try {
            FeignApiResponse<ClienteResponse> response = feignClient.buscarPorEmail(email);

            if (!response.success() || response.data() == null) {
                throw new ResourceNotFoundException("Cliente no encontrado con email: " + email);
            }

            ClienteResponse cr = response.data();
            String fullName = (cr.nombre() + " " + cr.apellidoPaterno() + " " + cr.apellidoMaterno()).trim();
            return new ClienteReferencia(cr.id(), cr.dni(), cr.nombre(), fullName, cr.email());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Cliente no encontrado con email: " + email);
        } catch (FeignException e) {
            log.error("Error calling clients-service for buscarPorEmail: {}", e.getMessage());
            throw new ExternalServiceException("Error al consultar clients-service: " + e.getMessage());
        }
    }

    public ClienteReferencia obtenerClienteFallback(UUID id, Throwable t) {
        log.warn("Fallback for obtenerCliente(id={}): {}", id, t.getMessage());
        throw new ExternalServiceException("clients-service no disponible para obtener cliente");
    }

    public List<ClienteReferencia> buscarPorNombreFallback(String nombre, Throwable t) {
        log.warn("Fallback for buscarPorNombre(nombre={}): {}", nombre, t.getMessage());
        throw new ExternalServiceException("clients-service no disponible para buscar clientes");
    }

    public ClienteReferencia buscarPorEmailFallback(String email, Throwable t) {
        log.warn("Fallback for buscarPorEmail(email={}): {}", email, t.getMessage());
        throw new ExternalServiceException("clients-service no disponible para buscar cliente por email");
    }
}
