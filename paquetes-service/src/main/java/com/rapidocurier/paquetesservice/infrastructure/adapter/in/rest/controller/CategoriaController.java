package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.controller;

import com.rapidocurier.paquetesservice.application.port.in.CategoriaRequest;
import com.rapidocurier.paquetesservice.application.port.in.ConsultarCategoriaUseCase;
import com.rapidocurier.paquetesservice.application.port.in.CrearCategoriaUseCase;
import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.dto.response.CategoriaResponse;
import com.rapidocurier.paquetesservice.infrastructure.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categories", description = "Package category catalog operations")
public class CategoriaController {

    private final ConsultarCategoriaUseCase consultarUseCase;
    private final CrearCategoriaUseCase crearUseCase;

    public CategoriaController(ConsultarCategoriaUseCase consultarUseCase,
                               CrearCategoriaUseCase crearUseCase) {
        this.consultarUseCase = consultarUseCase;
        this.crearUseCase = crearUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "List all categories", description = "Returns all available package categories")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of categories"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> listar() {
        List<CategoriaResponse> categorias = consultarUseCase.listarTodas().stream()
            .map(CategoriaResponse::fromDomain)
            .toList();
        return ApiResponse.ok(categorias);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new category", description = "Creates a new package category. Requires ADMIN role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category name already exists")
    })
    public ResponseEntity<ApiResponse<CategoriaResponse>> crear(
            @Valid @RequestBody CategoriaRequest request) {
        Categoria categoria = crearUseCase.crear(request.nombre(), request.descripcion());
        return ApiResponse.created(CategoriaResponse.fromDomain(categoria));
    }
}
