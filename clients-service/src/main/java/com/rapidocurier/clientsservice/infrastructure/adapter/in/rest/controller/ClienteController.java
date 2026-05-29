package com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.controller;

import com.rapidocurier.clientsservice.application.port.in.ConsultarClienteUseCase;
import com.rapidocurier.clientsservice.application.port.in.RegistrarClienteUseCase;
import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.dto.request.ClienteRequest;
import com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.dto.response.ClienteResponse;
import com.rapidocurier.clientsservice.infrastructure.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Clients", description = "Client CRUD operations")
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
    @Operation(summary = "Create a client", description = "Creates a new client with DNI and email, enriched with RENIEC data")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Client created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or OPERADOR role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Client already exists")
    })
    public ResponseEntity<ApiResponse<ClienteResponse>> crear(
            @Valid @RequestBody ClienteRequest request) {
        Cliente cliente = registrarClienteUseCase.registrar(request.dni(), request.email());
        return ApiResponse.created(ClienteResponse.fromDomain(cliente));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "List all clients", description = "Returns all registered clients")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of clients"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or OPERADOR role")
    })
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> listar() {
        List<ClienteResponse> clientes = consultarClienteUseCase.listarTodos().stream()
                .map(ClienteResponse::fromDomain)
                .toList();
        return ApiResponse.ok(clientes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Get client by ID", description = "Returns a single client by its UUID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Client found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or OPERADOR role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<ApiResponse<ClienteResponse>> obtener(@PathVariable UUID id) {
        Cliente cliente = consultarClienteUseCase.buscarPorId(id);
        return ApiResponse.ok(ClienteResponse.fromDomain(cliente));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a client", description = "Deletes a client by its UUID. Only ADMIN can delete.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Client deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        consultarClienteUseCase.eliminar(id);
        return ApiResponse.noContent();
    }
}
