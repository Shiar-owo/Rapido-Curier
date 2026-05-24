package com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.controller;

import com.rapidocurier.clientsservice.application.port.in.ConsultarClienteUseCase;
import com.rapidocurier.clientsservice.application.port.in.RegistrarClienteUseCase;
import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.dto.request.ClienteRequest;
import com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.dto.response.ClienteResponse;
import com.rapidocurier.clientsservice.infrastructure.common.ApiResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final RegistrarClienteUseCase registrarClienteUseCase;
    private final ConsultarClienteUseCase consultarClienteUseCase;

    public ClienteController(RegistrarClienteUseCase registrarClienteUseCase,
                             ConsultarClienteUseCase consultarClienteUseCase) {
        this.registrarClienteUseCase = registrarClienteUseCase;
        this.consultarClienteUseCase = consultarClienteUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<ClienteResponse>> crear(
            @Valid @RequestBody ClienteRequest request) {
        Cliente cliente = registrarClienteUseCase.registrar(request.dni(), request.email());
        return ApiResponse.created(ClienteResponse.fromDomain(cliente));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> listar() {
        List<ClienteResponse> clientes = consultarClienteUseCase.listarTodos().stream()
                .map(ClienteResponse::fromDomain)
                .toList();
        return ApiResponse.ok(clientes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtener(@PathVariable UUID id) {
        Cliente cliente = consultarClienteUseCase.buscarPorId(id);
        return ApiResponse.ok(ClienteResponse.fromDomain(cliente));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        consultarClienteUseCase.eliminar(id);
        return ApiResponse.noContent();
    }
}
