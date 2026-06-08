package com.rapidocurier.paquetesservice.infrastructure.adapter.in.rest.controller;

import com.rapidocurier.paquetesservice.application.port.in.ActualizarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.AsignarCategoriaUseCase;
import com.rapidocurier.paquetesservice.application.port.in.ConsultarMisPaquetesUseCase;
import com.rapidocurier.paquetesservice.application.port.in.ConsultarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.EliminarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.GestionarEstadoUseCase;
import com.rapidocurier.paquetesservice.application.port.in.PaqueteActualizarRequest;
import com.rapidocurier.paquetesservice.application.port.in.RegistrarPaqueteUseCase;
import com.rapidocurier.paquetesservice.domain.exception.ConflictException;
import com.rapidocurier.paquetesservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.paquetesservice.domain.exception.TransicionInvalidaException;
import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.application.port.in.PaqueteRequest;
import com.rapidocurier.paquetesservice.domain.port.out.ClienteFeignPort;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaqueteController.class)
@ContextConfiguration(classes = {
    PaqueteController.class,
    GlobalExceptionHandler.class,
    PaqueteControllerTest.TestSecurityConfig.class
})
@WithMockUser(roles = "ADMIN")
class PaqueteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrarPaqueteUseCase registrarUseCase;

    @MockitoBean
    private ConsultarPaqueteUseCase consultarUseCase;

    @MockitoBean
    private GestionarEstadoUseCase gestionarEstadoUseCase;

    @MockitoBean
    private ActualizarPaqueteUseCase actualizarUseCase;

    @MockitoBean
    private EliminarPaqueteUseCase eliminarUseCase;

    @MockitoBean
    private AsignarCategoriaUseCase asignarCategoriaUseCase;

    @MockitoBean
    private ConsultarMisPaquetesUseCase consultarMisPaquetesUseCase;

    @MockitoBean
    private ClienteFeignPort clienteFeignPort;

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

    private Paquete paqueteValido() {
        Categoria cat = new Categoria(UUID.randomUUID(), "FRAGIL", "Artículo frágil");
        return Paquete.create(
            UUID.randomUUID(), UUID.randomUUID(),
            5.0, 100.0,
            "LIMA", "AREQUIPA", 27.0,
            Set.of(cat)
        );
    }

    @Test
    void registrar_paqueteValido_retorna201() throws Exception {
        Paquete paquete = paqueteValido();
        when(registrarUseCase.registrar(any(PaqueteRequest.class))).thenReturn(paquete);

        mockMvc.perform(post("/api/v1/paquetes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "remitenteId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                        "destinatarioId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                        "pesoKg": 5.0,
                        "valorDeclarado": 100.0,
                        "sucursalOrigen": "LIMA",
                        "sucursalDestino": "AREQUIPA",
                        "categoriaIds": ["c1d2e3f4-a5b6-7890-cdef-123456789012"]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.codigoRastreo").isNotEmpty())
            .andExpect(jsonPath("$.data.estadoActual").value("REGISTRADO"));
    }

    @Test
    void registrar_categoriaNoExiste_retorna404() throws Exception {
        when(registrarUseCase.registrar(any(PaqueteRequest.class)))
            .thenThrow(new ResourceNotFoundException("Categoría no encontrada"));

        mockMvc.perform(post("/api/v1/paquetes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "remitenteId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                        "destinatarioId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                        "pesoKg": 5.0,
                        "valorDeclarado": 100.0,
                        "sucursalOrigen": "LIMA",
                        "sucursalDestino": "AREQUIPA",
                        "categoriaIds": ["c1d2e3f4-a5b6-7890-cdef-123456789012"]
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registrar_clientesNoExistentes_retorna404() throws Exception {
        when(registrarUseCase.registrar(any(PaqueteRequest.class)))
            .thenThrow(new ResourceNotFoundException("Cliente no encontrado: " + UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/paquetes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "remitenteId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                        "destinatarioId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                        "pesoKg": 5.0,
                        "valorDeclarado": 100.0,
                        "sucursalOrigen": "LIMA",
                        "sucursalDestino": "AREQUIPA",
                        "categoriaIds": ["c1d2e3f4-a5b6-7890-cdef-123456789012"]
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void buscarPorId_existe_retorna200() throws Exception {
        Paquete paquete = paqueteValido();
        when(consultarUseCase.buscarPorId(paquete.getId())).thenReturn(paquete);

        mockMvc.perform(get("/api/v1/paquetes/{id}", paquete.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(paquete.getId().toString()));
    }

    @Test
    void buscarPorId_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarUseCase.buscarPorId(id))
            .thenThrow(new ResourceNotFoundException("Paquete no encontrado: " + id));

        mockMvc.perform(get("/api/v1/paquetes/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void buscarPorCodigoRastreo_retorna200() throws Exception {
        Paquete paquete = paqueteValido();
        when(consultarUseCase.buscarPorCodigoRastreo("RC2026")).thenReturn(List.of(paquete));

        mockMvc.perform(get("/api/v1/paquetes/buscar").param("texto", "RC2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].codigoRastreo").isNotEmpty());
    }

    @Test
    void cambiarEstado_transicionValida_retorna200() throws Exception {
        Paquete paquete = paqueteValido();

        mockMvc.perform(put("/api/v1/paquetes/{id}/estado", paquete.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "nuevoEstado": "EN_ALMACEN",
                        "usuarioResponsable": "operador1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cambiarEstado_transicionInvalida_retorna400() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new TransicionInvalidaException(EstadoPaquete.REGISTRADO, EstadoPaquete.ENTREGADO))
            .when(gestionarEstadoUseCase).cambiarEstado(eq(id), any(), any());

        mockMvc.perform(put("/api/v1/paquetes/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "nuevoEstado": "ENTREGADO",
                        "usuarioResponsable": "operador1"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cambiarEstado_paqueteNoExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Paquete no encontrado: " + id))
            .when(gestionarEstadoUseCase).cambiarEstado(eq(id), any(), any());

        mockMvc.perform(put("/api/v1/paquetes/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "nuevoEstado": "EN_ALMACEN",
                        "usuarioResponsable": "operador1"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void obtenerHistorial_paqueteExiste_retorna200() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        List<EstadoHistorial> historial = List.of(
            new EstadoHistorial(null, paqueteId, EstadoPaquete.REGISTRADO, OffsetDateTime.now(), "sistema"),
            new EstadoHistorial(null, paqueteId, EstadoPaquete.EN_ALMACEN, OffsetDateTime.now(), "operador1")
        );
        when(gestionarEstadoUseCase.obtenerHistorial(paqueteId)).thenReturn(historial);

        mockMvc.perform(get("/api/v1/paquetes/{id}/historial", paqueteId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void obtenerHistorial_paqueteNoExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(gestionarEstadoUseCase.obtenerHistorial(id))
            .thenThrow(new ResourceNotFoundException("Paquete no encontrado: " + id));

        mockMvc.perform(get("/api/v1/paquetes/{id}/historial", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void actualizar_paqueteValido_retorna200() throws Exception {
        Paquete paquete = paqueteValido();
        when(actualizarUseCase.actualizar(eq(paquete.getId()), any(PaqueteActualizarRequest.class)))
            .thenReturn(paquete);

        mockMvc.perform(put("/api/v1/paquetes/{id}", paquete.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "pesoKg": 7.5,
                        "valorDeclarado": 200.0,
                        "sucursalOrigen": "LIMA",
                        "sucursalDestino": "CUSCO"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(paquete.getId().toString()));
    }

    @Test
    void actualizar_paqueteNoExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(actualizarUseCase.actualizar(eq(id), any(PaqueteActualizarRequest.class)))
            .thenThrow(new ResourceNotFoundException("Paquete no encontrado: " + id));

        mockMvc.perform(put("/api/v1/paquetes/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "pesoKg": 7.5,
                        "valorDeclarado": 200.0
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void eliminar_paqueteExiste_retorna204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/paquetes/{id}", id))
            .andExpect(status().isNoContent())
            .andExpect(jsonPath("$.success").value(true));

        verify(eliminarUseCase).eliminar(id);
    }

    @Test
    void eliminar_paqueteNoExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Paquete no encontrado: " + id))
            .when(eliminarUseCase).eliminar(id);

        mockMvc.perform(delete("/api/v1/paquetes/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void asignarCategoria_happyPath_retorna200() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/paquetes/{id}/categorias/{categoriaId}", paqueteId, categoriaId))
            .andExpect(status().isNoContent())
            .andExpect(jsonPath("$.success").value(true));

        verify(asignarCategoriaUseCase).asignarCategoria(paqueteId, categoriaId);
    }

    @Test
    void asignarCategoria_paqueteNoExiste_retorna404() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Paquete no encontrado: " + paqueteId))
            .when(asignarCategoriaUseCase).asignarCategoria(paqueteId, categoriaId);

        mockMvc.perform(post("/api/v1/paquetes/{id}/categorias/{categoriaId}", paqueteId, categoriaId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void asignarCategoria_categoriaYaAsignada_retorna409() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        doThrow(new ConflictException("La categoría ya está asignada al paquete"))
            .when(asignarCategoriaUseCase).asignarCategoria(paqueteId, categoriaId);

        mockMvc.perform(post("/api/v1/paquetes/{id}/categorias/{categoriaId}", paqueteId, categoriaId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", roles = "CLIENTE")
    void misPaquetes_happyPath_retorna200() throws Exception {
        Paquete paquete = paqueteValido();
        when(consultarMisPaquetesUseCase.buscarMisPaquetes(
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")))
            .thenReturn(List.of(paquete));

        mockMvc.perform(get("/api/v1/paquetes/mis-paquetes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", roles = "CLIENTE")
    void misPaquetes_sinPaquetes_retorna200Vacia() throws Exception {
        when(consultarMisPaquetesUseCase.buscarMisPaquetes(
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/paquetes/mis-paquetes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", roles = "ADMIN")
    void misPaquetes_rolAdmin_retorna403() throws Exception {
        mockMvc.perform(get("/api/v1/paquetes/mis-paquetes"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", roles = "CLIENTE")
    void misPaquetesHistorial_happyPath_retorna200() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID clienteId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        List<EstadoHistorial> historial = List.of(
            new EstadoHistorial(null, paqueteId, EstadoPaquete.REGISTRADO, OffsetDateTime.now(), "sistema")
        );
        when(consultarMisPaquetesUseCase.obtenerHistorialMisPaquetes(clienteId, paqueteId))
            .thenReturn(historial);

        mockMvc.perform(get("/api/v1/paquetes/mis-paquetes/{id}/historial", paqueteId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", roles = "CLIENTE")
    void misPaquetesHistorial_paqueteNoExiste_retorna404() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        when(consultarMisPaquetesUseCase.obtenerHistorialMisPaquetes(
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"), paqueteId))
            .thenThrow(new ResourceNotFoundException("Paquete no encontrado: " + paqueteId));

        mockMvc.perform(get("/api/v1/paquetes/mis-paquetes/{id}/historial", paqueteId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", roles = "CLIENTE")
    void misPaquetesHistorial_noEsPropietario_retorna404() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        when(consultarMisPaquetesUseCase.obtenerHistorialMisPaquetes(
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"), paqueteId))
            .thenThrow(new ResourceNotFoundException("Paquete no encontrado: " + paqueteId));

        mockMvc.perform(get("/api/v1/paquetes/mis-paquetes/{id}/historial", paqueteId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }
}
