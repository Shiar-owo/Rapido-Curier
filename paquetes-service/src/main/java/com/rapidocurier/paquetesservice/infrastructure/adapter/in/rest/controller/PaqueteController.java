package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.controller;

import com.rapidocurier.paquetesservice.application.port.in.ActualizarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.ConsultarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.EliminarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.GestionarEstadoUseCase;
import com.rapidocurier.paquetesservice.application.port.in.PaqueteActualizarRequest;
import com.rapidocurier.paquetesservice.application.port.in.PaqueteRequest;
import com.rapidocurier.paquetesservice.application.port.in.RegistrarPaqueteUseCase;
import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.request.CambiarEstadoRequest;
import com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.request.PaqueteRegistrarRequest;
import com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.response.EstadoHistorialResponse;
import com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.response.PaqueteResponse;
import com.rapidocurier.paquetesservice.infrastructure.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/paquetes")
@RequiredArgsConstructor
@Tag(name = "Packages", description = "Package registration and tracking operations")
public class PaqueteController {

    private final RegistrarPaqueteUseCase registrarUseCase;
    private final ConsultarPaqueteUseCase consultarUseCase;
    private final GestionarEstadoUseCase gestionarEstadoUseCase;
    private final ActualizarPaqueteUseCase actualizarUseCase;
    private final EliminarPaqueteUseCase eliminarUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Register a new package", description = "Registers a new package with sender, recipient, categories and branch info")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Package registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or OPERADOR role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Client or category not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Empty categories")
    })
    public ResponseEntity<ApiResponse<PaqueteResponse>> registrar(
            @Valid @RequestBody PaqueteRegistrarRequest request) {
        PaqueteRequest paqueteRequest = new PaqueteRequest(
            request.remitenteId(), request.destinatarioId(),
            request.pesoKg(), request.valorDeclarado(),
            request.sucursalOrigen(), request.sucursalDestino(),
            request.categoriaIds()
        );
        Paquete paquete = registrarUseCase.registrar(paqueteRequest);
        return ApiResponse.created(PaqueteResponse.fromDomain(paquete));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'REPARTidor')")
    @Operation(summary = "Get package by ID", description = "Returns a single package by its UUID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Package found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package not found")
    })
    public ResponseEntity<ApiResponse<PaqueteResponse>> buscarPorId(@PathVariable UUID id) {
        Paquete paquete = consultarUseCase.buscarPorId(id);
        return ApiResponse.ok(PaqueteResponse.fromDomain(paquete));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Search packages by tracking code", description = "Returns packages matching the given tracking code text")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of matching packages"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<PaqueteResponse>>> buscarPorCodigoRastreo(
            @RequestParam String texto) {
        List<PaqueteResponse> paquetes = consultarUseCase.buscarPorCodigoRastreo(texto).stream()
            .map(PaqueteResponse::fromDomain)
            .toList();
        return ApiResponse.ok(paquetes);
    }

    @GetMapping("/sucursal/{sucursal}/estado/{estado}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Search packages by branch and status", description = "Returns packages for a branch in a given status")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of matching packages"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<PaqueteResponse>>> buscarPorSucursalYEstado(
            @PathVariable String sucursal,
            @PathVariable EstadoPaquete estado) {
        List<PaqueteResponse> paquetes = consultarUseCase.buscarPorSucursalYEstado(sucursal, estado).stream()
            .map(PaqueteResponse::fromDomain)
            .toList();
        return ApiResponse.ok(paquetes);
    }

    @GetMapping("/cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Search packages by sender or recipient name", description = "Returns packages where the sender or recipient matches the given name")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of matching packages"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<PaqueteResponse>>> buscarPorRemitenteOrDestinatario(
            @RequestParam String nombre) {
        List<PaqueteResponse> paquetes = consultarUseCase.buscarPorRemitenteOrDestinatario(nombre).stream()
            .map(PaqueteResponse::fromDomain)
            .toList();
        return ApiResponse.ok(paquetes);
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Change package status", description = "Changes the status of a package following valid state transitions")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status changed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package not found")
    })
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        gestionarEstadoUseCase.cambiarEstado(id, request.nuevoEstado(), request.usuarioResponsable());
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'REPARTidor')")
    @Operation(summary = "Get package status history", description = "Returns the status change history for a package")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status history"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package not found")
    })
    public ResponseEntity<ApiResponse<List<EstadoHistorialResponse>>> obtenerHistorial(
            @PathVariable UUID id) {
        List<EstadoHistorialResponse> historial = gestionarEstadoUseCase.obtenerHistorial(id).stream()
            .map(EstadoHistorialResponse::fromDomain)
            .toList();
        return ApiResponse.ok(historial);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Update package", description = "Updates package fields (weight, declared value, branches). Tariff is recalculated automatically.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Package updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or OPERADOR role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package not found")
    })
    public ResponseEntity<ApiResponse<PaqueteResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PaqueteActualizarRequest request) {
        Paquete paquete = actualizarUseCase.actualizar(id, request);
        return ApiResponse.ok(PaqueteResponse.fromDomain(paquete));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete package", description = "Deletes a package. Requires ADMIN role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Package deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package not found")
    })
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        eliminarUseCase.eliminar(id);
        return ApiResponse.noContent();
    }
}
