package com.rapidocurier.authservice.infrastructure.adapter.in.rest.controller;

import com.rapidocurier.authservice.application.port.in.LoginUseCase;
import com.rapidocurier.authservice.application.port.in.RegisterUseCase;
import com.rapidocurier.authservice.domain.model.Usuario;
import com.rapidocurier.authservice.domain.port.out.JwtPort;
import com.rapidocurier.authservice.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.rapidocurier.authservice.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.rapidocurier.authservice.infrastructure.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final JwtPort jwtPort;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a user account and returns a JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        Usuario usuario = registerUseCase.registrar(
                request.nombre(), request.email(), request.password(), request.rol());
        String token = jwtPort.generarToken(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(token));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates a user and returns a JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.ok(token));
    }
}