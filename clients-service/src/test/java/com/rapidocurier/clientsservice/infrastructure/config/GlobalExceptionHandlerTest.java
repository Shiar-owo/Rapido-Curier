package com.rapidocurier.clientsservice.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidocurier.clientsservice.domain.exception.ConflictException;
import com.rapidocurier.clientsservice.domain.exception.ExternalServiceException;
import com.rapidocurier.clientsservice.domain.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {
    GlobalExceptionHandlerTest.TestController.class,
    GlobalExceptionHandler.class,
    GlobalExceptionHandlerTest.TestSecurityConfig.class
})
@WithMockUser
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
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

    @RestController
    static class TestController {

        @GetMapping("/throw/conflict")
        void throwConflict() {
            throw new ConflictException("El email ya está registrado");
        }

        @GetMapping("/throw/not-found")
        void throwNotFound() {
            throw new ResourceNotFoundException("Cliente no encontrado");
        }

        @GetMapping("/throw/external-service")
        void throwExternalService() {
            throw new ExternalServiceException("RENIEC no disponible");
        }

        @GetMapping("/throw/access-denied")
        void throwAccessDenied() {
            throw new AccessDeniedException("Acceso denegado");
        }

        @GetMapping("/throw/generic")
        void throwGeneric() {
            throw new RuntimeException("Error inesperado");
        }

        @PostMapping("/throw/validation")
        void throwValidation(@Valid @RequestBody DummyRequest request) {}

        record DummyRequest(@NotBlank String campo) {}
    }

    @Test
    void conflictException_retorna409() throws Exception {
        mockMvc.perform(get("/throw/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("El email ya está registrado"));
    }

    @Test
    void resourceNotFoundException_retorna404() throws Exception {
        mockMvc.perform(get("/throw/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Cliente no encontrado"));
    }

    @Test
    void externalServiceException_retorna503() throws Exception {
        mockMvc.perform(get("/throw/external-service"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("RENIEC no disponible"));
    }

    @Test
    void accessDeniedException_retorna403() throws Exception {
        mockMvc.perform(get("/throw/access-denied"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Acceso denegado"));
    }

    @Test
    void genericException_retorna500() throws Exception {
        mockMvc.perform(get("/throw/generic"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Error interno del servidor"));
    }

    @Test
    void validationException_retorna400() throws Exception {
        mockMvc.perform(post("/throw/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Validación fallida"))
            .andExpect(jsonPath("$.data.campo").isArray());
    }
}
