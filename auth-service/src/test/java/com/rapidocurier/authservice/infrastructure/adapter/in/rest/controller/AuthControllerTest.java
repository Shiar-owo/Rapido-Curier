package com.rapidocurier.authservice.infrastructure.adapter.in.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidocurier.authservice.application.port.in.LoginUseCase;
import com.rapidocurier.authservice.application.port.in.RegisterUseCase;
import com.rapidocurier.authservice.domain.exception.ConflictException;
import com.rapidocurier.authservice.domain.exception.CredencialesInvalidasException;
import com.rapidocurier.authservice.domain.model.Usuario;
import com.rapidocurier.authservice.domain.port.out.JwtPort;
import com.rapidocurier.authservice.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.rapidocurier.authservice.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.rapidocurier.authservice.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private JwtPort jwtPort;

    @Test
    void register_exitoso_retorna201YToken() throws Exception {
        RegisterRequest request = new RegisterRequest("Juan", "juan@test.com", "password123", "CLIENTE");
        Usuario usuario = new Usuario("Juan", "encoded", "juan@test.com", Set.of("CLIENTE"));

        when(registerUseCase.registrar(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(usuario);
        when(jwtPort.generarToken(any())).thenReturn("jwt.token.test");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("jwt.token.test"));
    }

    @Test
    void register_emailDuplicado_retorna409() throws Exception {
        RegisterRequest request = new RegisterRequest("Juan", "juan@test.com", "password123", "CLIENTE");

        when(registerUseCase.registrar(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ConflictException("El email ya está registrado"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_datosInvalidos_retorna400() throws Exception {
        RegisterRequest request = new RegisterRequest("", "email-invalido", "", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_exitoso_retorna200YToken() throws Exception {
        LoginRequest request = new LoginRequest("juan@test.com", "password123");

        when(loginUseCase.login(anyString(), anyString())).thenReturn("jwt.token.test");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("jwt.token.test"));
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        LoginRequest request = new LoginRequest("juan@test.com", "wrongpassword");

        when(loginUseCase.login(anyString(), anyString()))
                .thenThrow(new CredencialesInvalidasException("Credenciales inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_emailVacio_retorna400() throws Exception {
        LoginRequest request = new LoginRequest("", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}