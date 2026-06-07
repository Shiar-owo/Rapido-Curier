package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.controller;

import com.rapidocurier.paquetesservice.application.port.in.ConsultarCategoriaUseCase;
import com.rapidocurier.paquetesservice.application.port.in.CrearCategoriaUseCase;
import com.rapidocurier.paquetesservice.domain.exception.ConflictException;
import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.infrastructure.config.GlobalExceptionHandler;

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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriaController.class)
@ContextConfiguration(classes = {
    CategoriaController.class,
    GlobalExceptionHandler.class,
    CategoriaControllerTest.TestSecurityConfig.class
})
@WithMockUser(roles = "ADMIN")
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarCategoriaUseCase consultarUseCase;

    @MockitoBean
    private CrearCategoriaUseCase crearUseCase;

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

    @Test
    void listar_conCategorias_retorna200() throws Exception {
        List<Categoria> categorias = List.of(
            new Categoria(UUID.randomUUID(), "FRAGIL", "Artículo frágil"),
            new Categoria(UUID.randomUUID(), "DOCUMENTO", "Documento importante")
        );
        when(consultarUseCase.listarTodas()).thenReturn(categorias);

        mockMvc.perform(get("/api/v1/categorias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].nombre").value("FRAGIL"));
    }

    @Test
    void listar_sinCategorias_retorna200Vacio() throws Exception {
        when(consultarUseCase.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categorias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void crear_categoriaValida_retorna201() throws Exception {
        Categoria categoria = new Categoria(UUID.randomUUID(), "FRAGIL", "Artículo frágil");
        when(crearUseCase.crear(any(String.class), any(String.class))).thenReturn(categoria);

        mockMvc.perform(post("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "nombre": "FRAGIL",
                        "descripcion": "Artículo frágil"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").isNotEmpty())
            .andExpect(jsonPath("$.data.nombre").value("FRAGIL"))
            .andExpect(jsonPath("$.data.descripcion").value("Artículo frágil"));
    }

    @Test
    void crear_nombreDuplicado_retorna409() throws Exception {
        when(crearUseCase.crear(any(String.class), any(String.class)))
            .thenThrow(new ConflictException("La categoría 'FRAGIL' ya existe"));

        mockMvc.perform(post("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "nombre": "FRAGIL",
                        "descripcion": "Artículo frágil"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void crear_rolOperador_retorna403() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "nombre": "FRAGIL",
                        "descripcion": "Artículo frágil"
                    }
                    """))
            .andExpect(status().isForbidden());
    }
}
