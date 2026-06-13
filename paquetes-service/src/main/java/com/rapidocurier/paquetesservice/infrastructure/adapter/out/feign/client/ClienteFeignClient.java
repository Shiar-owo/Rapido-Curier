package com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.client;

import com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.dto.FeignApiResponse;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.dto.ClienteResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "clients-service")
public interface ClienteFeignClient {

    @GetMapping("/api/v1/clientes/{id}")
    FeignApiResponse<ClienteResponse> obtenerPorId(@PathVariable UUID id);

    @GetMapping("/api/v1/clientes/buscar")
    FeignApiResponse<List<ClienteResponse>> buscarPorNombre(@RequestParam("nombre") String nombre);

    @GetMapping("/api/v1/clientes/por-email")
    FeignApiResponse<ClienteResponse> buscarPorEmail(@RequestParam("email") String email);
}
