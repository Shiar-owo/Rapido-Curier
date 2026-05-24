package com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidocurier.clientsservice.application.port.in.ConsultarClienteUseCase;
import com.rapidocurier.clientsservice.application.port.in.RegistrarClienteUseCase;
import com.rapidocurier.clientsservice.domain.exception.ConflictException;
import com.rapidocurier.clientsservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.infrastructure.adapter.in.rest.dto.request.ClienteRequest;
import com.rapidocurier.clientsservice.infrastructure.config.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@ContextConfiguration(classes = {
    ClienteController.class,
    GlobalExceptionHandler.class,
    ClienteControllerTest.TestSecurityConfig.class
})
@WithMockUser(roles = "ADMIN")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegistrarClienteUseCase registrarClienteUseCase;

    @MockitoBean
    private ConsultarClienteUseCase consultarClienteUseCase;

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
        }
    }

    private Cliente clienteValido() {
        return Cliente.rehydrate(
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            "46027897",
            "ROXANA KARINA",
            "DELGADO",
            "HUAMANI",
            "roxana@email.com",
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
    }

    @Test
    void crear_clienteValido_retorna201() throws Exception {
        when(registrarClienteUseCase.registrar("46027897", "roxana@email.com"))
            .thenReturn(clienteValido());

        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ClienteRequest("46027897", "roxana@email.com"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
            .andExpect(jsonPath("$.data.dni").value("46027897"))
            .andExpect(jsonPath("$.data.nombre").value("ROXANA KARINA"))
            .andExpect(jsonPath("$.data.apellidoPaterno").value("DELGADO"))
            .andExpect(jsonPath("$.data.apellidoMaterno").value("HUAMANI"))
            .andExpect(jsonPath("$.data.email").value("roxana@email.com"));
    }

    @Test
    void crear_dniInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ClienteRequest("123", "email@test.com"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Validación fallida"));
    }

    @Test
    void crear_emailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ClienteRequest("46027897", "email-invalido"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void crear_emailDuplicado_retorna409() throws Exception {
        when(registrarClienteUseCase.registrar("46027897", "dup@email.com"))
            .thenThrow(new ConflictException("El email ya está registrado"));

        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ClienteRequest("46027897", "dup@email.com"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("El email ya está registrado"));
    }

    @Test
    void listar_conClientes_retorna200() throws Exception {
        when(consultarClienteUseCase.listarTodos()).thenReturn(List.of(clienteValido()));

        mockMvc.perform(get("/api/v1/clientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].dni").value("46027897"));
    }

    @Test
    void listar_sinClientes_retorna200() throws Exception {
        when(consultarClienteUseCase.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/clientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void obtener_porIdExistente_retorna200() throws Exception {
        UUID id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        when(consultarClienteUseCase.buscarPorId(id)).thenReturn(clienteValido());

        mockMvc.perform(get("/api/v1/clientes/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(id.toString()))
            .andExpect(jsonPath("$.data.email").value("roxana@email.com"));
    }

    @Test
    void obtener_porIdInexistente_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarClienteUseCase.buscarPorId(id))
            .thenThrow(new ResourceNotFoundException("Cliente no encontrado"));

        mockMvc.perform(get("/api/v1/clientes/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Cliente no encontrado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminar_porIdExistente_retorna204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/clientes/{id}", id))
            .andExpect(status().isNoContent())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminar_porIdInexistente_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Cliente no encontrado"))
            .when(consultarClienteUseCase).eliminar(id);

        mockMvc.perform(delete("/api/v1/clientes/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void eliminar_conRolOperador_retorna403() throws Exception {
        mockMvc.perform(delete("/api/v1/clientes/{id}", UUID.randomUUID()))
            .andExpect(status().isForbidden());
    }
}
