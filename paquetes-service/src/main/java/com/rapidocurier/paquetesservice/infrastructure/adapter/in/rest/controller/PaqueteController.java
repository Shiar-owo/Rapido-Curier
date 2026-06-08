package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.controller;

import com.rapidocurier.paquetesservice.application.port.in.ActualizarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.AsignarCategoriaUseCase;
import com.rapidocurier.paquetesservice.application.port.in.ConsultarMisPaquetesUseCase;
import com.rapidocurier.paquetesservice.application.port.in.ConsultarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.EliminarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.GestionarEstadoUseCase;
import com.rapidocurier.paquetesservice.application.port.in.PaqueteActualizarRequest;
import com.rapidocurier.paquetesservice.application.port.in.PaqueteRequest;
import com.rapidocurier.paquetesservice.application.port.in.RegistrarPaqueteUseCase;
import com.rapidocurier.paquetesservice.domain.model.ClienteReferencia;
import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.domain.port.out.ClienteFeignPort;
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
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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

@Slf4j
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
    private final AsignarCategoriaUseCase asignarCategoriaUseCase;
    private final ConsultarMisPaquetesUseCase consultarMisPaquetesUseCase;
    private final ClienteFeignPort clienteFeignPort;

    private PaqueteResponse enrichWithClientNames(Paquete paquete) {
        String remitenteNombre = null;
        String destinatarioNombre = null;
        try {
            ClienteReferencia remitente = clienteFeignPort.obtenerCliente(paquete.getRemitenteId());
            remitenteNombre = remitente.nombreCompleto();
        } catch (Exception e) { log.warn("No se pudo obtener nombre del remitente {}: {}", paquete.getRemitenteId(), e.getMessage()); }
        try {
            ClienteReferencia destinatario = clienteFeignPort.obtenerCliente(paquete.getDestinatarioId());
            destinatarioNombre = destinatario.nombreCompleto();
        } catch (Exception e) { log.warn("No se pudo obtener nombre del destinatario {}: {}", paquete.getDestinatarioId(), e.getMessage()); }
        return PaqueteResponse.fromDomain(paquete, remitenteNombre, destinatarioNombre);
    }

    private List<PaqueteResponse> enrichAll(List<Paquete> paquetes) {
        return paquetes.stream().map(this::enrichWithClientNames).toList();
    }

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
        return ApiResponse.created(enrichWithClientNames(paquete));
    }

    @GetMapping("/mis-paquetes")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Get my packages", description = "Returns all packages where the authenticated user is sender or recipient. Requires CLIENTE role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of packages"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires CLIENTE role")
    })
    public ResponseEntity<ApiResponse<List<PaqueteResponse>>> misPaquetes() {
        UUID clienteId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        List<PaqueteResponse> paquetes = enrichAll(consultarMisPaquetesUseCase.buscarMisPaquetes(clienteId));
        return ApiResponse.ok(paquetes);
    }

    @GetMapping("/mis-paquetes/{id}/historial")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Get history of my package", description = "Returns the status history of a package owned by the authenticated user. Requires CLIENTE role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of status changes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires CLIENTE role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package not found or not owned by user")
    })
    public ResponseEntity<ApiResponse<List<EstadoHistorialResponse>>> misPaquetesHistorial(@PathVariable UUID id) {
        UUID clienteId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        List<EstadoHistorialResponse> historial = consultarMisPaquetesUseCase.obtenerHistorialMisPaquetes(clienteId, id).stream()
            .map(EstadoHistorialResponse::fromDomain)
            .toList();
        return ApiResponse.ok(historial);
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
        return ApiResponse.ok(enrichWithClientNames(paquete));
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
        List<PaqueteResponse> paquetes = enrichAll(consultarUseCase.buscarPorCodigoRastreo(texto));
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
        List<PaqueteResponse> paquetes = enrichAll(consultarUseCase.buscarPorSucursalYEstado(sucursal, estado));
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
        List<PaqueteResponse> paquetes = enrichAll(consultarUseCase.buscarPorRemitenteOrDestinatario(nombre));
        return ApiResponse.ok(paquetes);
    }

    @GetMapping("/por-categoria")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Search packages by category name", description = "Returns packages that have the given category, using a JPQL JOIN between Paquete and Categoria entities")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of matching packages"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<PaqueteResponse>>> buscarPorCategoria(
            @RequestParam String nombre) {
        List<PaqueteResponse> paquetes = enrichAll(consultarUseCase.buscarPorCategoriaNombre(nombre));
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
        return ApiResponse.ok(enrichWithClientNames(paquete));
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

    @PostMapping("/{id}/categorias/{categoriaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Assign category to package", description = "Adds a category to an existing package. Requires ADMIN or OPERADOR role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category assigned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or OPERADOR role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Package or category not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category already assigned to package")
    })
    public ResponseEntity<ApiResponse<Void>> asignarCategoria(
            @PathVariable UUID id,
            @PathVariable UUID categoriaId) {
        asignarCategoriaUseCase.asignarCategoria(id, categoriaId);
        return ApiResponse.noContent();
    }
}
