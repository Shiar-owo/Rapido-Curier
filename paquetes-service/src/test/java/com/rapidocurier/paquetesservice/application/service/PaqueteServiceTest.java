package com.rapidocurier.paquetesservice.application.service;

import com.rapidocurier.paquetesservice.application.port.in.PaqueteActualizarRequest;
import com.rapidocurier.paquetesservice.application.port.in.PaqueteRequest;
import com.rapidocurier.paquetesservice.domain.exception.ConflictException;
import com.rapidocurier.paquetesservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.paquetesservice.domain.exception.TransicionInvalidaException;
import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.model.ClienteReferencia;
import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.domain.port.out.CategoriaRepositoryPort;
import com.rapidocurier.paquetesservice.domain.port.out.ClienteFeignPort;
import com.rapidocurier.paquetesservice.domain.port.out.HistorialRepositoryPort;
import com.rapidocurier.paquetesservice.domain.port.out.PaqueteRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaqueteServiceTest {

    @Mock
    private PaqueteRepositoryPort repo;

    @Mock
    private HistorialRepositoryPort historial;

    @Mock
    private CategoriaRepositoryPort categoriaRepo;

    @Mock
    private ClienteFeignPort clienteFeign;

    @Mock
    private TarifaCalculator tarifaCalculator;

    private PaqueteService service;

    private UUID remitenteId;
    private UUID destinatarioId;
    private Paquete paquete;
    private Categoria categoriaBase;

    @BeforeEach
    void setUp() {
        service = new PaqueteService(repo, historial, categoriaRepo, clienteFeign, tarifaCalculator);

        remitenteId = UUID.randomUUID();
        destinatarioId = UUID.randomUUID();
        categoriaBase = new Categoria(UUID.randomUUID(), "FRAGIL", "Artículo frágil");

        paquete = Paquete.create(
            remitenteId, destinatarioId,
            5.0, 100.0,
            "LIMA", "AREQUIPA", 27.0,
            Set.of(categoriaBase)
        );
    }

    @Test
    void registrar_happyPath_guardaPaqueteYHistorial() {
        UUID catId = UUID.randomUUID();
        Categoria categoria = new Categoria(catId, "FRAGIL", "Artículo frágil");

        when(clienteFeign.obtenerCliente(remitenteId)).thenReturn(
            new ClienteReferencia(remitenteId, "12345678", "Juan", "juan@test.com"));
        when(clienteFeign.obtenerCliente(destinatarioId)).thenReturn(
            new ClienteReferencia(destinatarioId, "87654321", "Maria", "maria@test.com"));
        when(categoriaRepo.buscarPorId(catId)).thenReturn(Optional.of(categoria));
        when(tarifaCalculator.calcular(5.0, 100.0, "LIMA", "AREQUIPA")).thenReturn(27.0);
        when(repo.guardar(any(Paquete.class))).thenReturn(paquete);

        PaqueteRequest request = new PaqueteRequest(
            remitenteId, destinatarioId,
            5.0, 100.0,
            "LIMA", "AREQUIPA",
            Set.of(catId)
        );

        Paquete result = service.registrar(request);

        assertAll(
            () -> assertEquals(27.0, result.getTarifa()),
            () -> assertEquals(EstadoPaquete.REGISTRADO, result.getEstadoActual()),
            () -> assertEquals(remitenteId, result.getRemitenteId()),
            () -> assertEquals(destinatarioId, result.getDestinatarioId()),
            () -> assertNotNull(result.getCodigoRastreo()),
            () -> assertTrue(result.getCodigoRastreo().startsWith("RC"))
        );

        verify(clienteFeign).obtenerCliente(remitenteId);
        verify(clienteFeign).obtenerCliente(destinatarioId);

        ArgumentCaptor<EstadoHistorial> historialCaptor = ArgumentCaptor.forClass(EstadoHistorial.class);
        verify(historial).guardar(historialCaptor.capture());
        assertEquals(EstadoPaquete.REGISTRADO, historialCaptor.getValue().getEstado());
        assertEquals("sistema", historialCaptor.getValue().getUsuarioResponsable());
    }

    @Test
    void registrar_remitenteNoExiste_lanzaResourceNotFoundException() {
        when(clienteFeign.obtenerCliente(remitenteId))
            .thenThrow(new ResourceNotFoundException("Cliente no encontrado: " + remitenteId));

        PaqueteRequest request = new PaqueteRequest(
            remitenteId, destinatarioId,
            5.0, 100.0,
            "LIMA", "AREQUIPA",
            Set.of(UUID.randomUUID())
        );

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(request));
        verify(repo, never()).guardar(any());
    }

    @Test
    void registrar_destinatarioNoExiste_lanzaResourceNotFoundException() {
        when(clienteFeign.obtenerCliente(remitenteId)).thenReturn(
            new ClienteReferencia(remitenteId, "12345678", "Juan", "juan@test.com"));
        when(clienteFeign.obtenerCliente(destinatarioId))
            .thenThrow(new ResourceNotFoundException("Cliente no encontrado: " + destinatarioId));

        PaqueteRequest request = new PaqueteRequest(
            remitenteId, destinatarioId,
            5.0, 100.0,
            "LIMA", "AREQUIPA",
            Set.of(UUID.randomUUID())
        );

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(request));
        verify(repo, never()).guardar(any());
    }

    @Test
    void registrar_sinCategorias_lanzaConflictException() {
        when(clienteFeign.obtenerCliente(remitenteId)).thenReturn(
            new ClienteReferencia(remitenteId, "12345678", "Juan", "juan@test.com"));
        when(clienteFeign.obtenerCliente(destinatarioId)).thenReturn(
            new ClienteReferencia(destinatarioId, "87654321", "Maria", "maria@test.com"));

        PaqueteRequest request = new PaqueteRequest(
            remitenteId, destinatarioId,
            5.0, 100.0,
            "LIMA", "AREQUIPA",
            Set.of()
        );

        assertThrows(ConflictException.class, () -> service.registrar(request));
        verify(repo, never()).guardar(any());
    }

    @Test
    void registrar_categoriasNull_lanzaConflictException() {
        when(clienteFeign.obtenerCliente(remitenteId)).thenReturn(
            new ClienteReferencia(remitenteId, "12345678", "Juan", "juan@test.com"));
        when(clienteFeign.obtenerCliente(destinatarioId)).thenReturn(
            new ClienteReferencia(destinatarioId, "87654321", "Maria", "maria@test.com"));

        PaqueteRequest request = new PaqueteRequest(
            remitenteId, destinatarioId,
            5.0, 100.0,
            "LIMA", "AREQUIPA",
            null
        );

        assertThrows(ConflictException.class, () -> service.registrar(request));
        verify(repo, never()).guardar(any());
    }

    @Test
    void registrar_categoriaNoExiste_lanzaResourceNotFoundException() {
        UUID catId = UUID.randomUUID();
        when(clienteFeign.obtenerCliente(remitenteId)).thenReturn(
            new ClienteReferencia(remitenteId, "12345678", "Juan", "juan@test.com"));
        when(clienteFeign.obtenerCliente(destinatarioId)).thenReturn(
            new ClienteReferencia(destinatarioId, "87654321", "Maria", "maria@test.com"));
        when(categoriaRepo.buscarPorId(catId)).thenReturn(Optional.empty());

        PaqueteRequest request = new PaqueteRequest(
            remitenteId, destinatarioId,
            5.0, 100.0,
            "LIMA", "AREQUIPA",
            Set.of(catId)
        );

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(request));
        verify(repo, never()).guardar(any());
    }

    @Test
    void buscarPorId_existe_retornaPaquete() {
        when(repo.buscarPorId(paquete.getId())).thenReturn(Optional.of(paquete));

        Paquete result = service.buscarPorId(paquete.getId());

        assertEquals(paquete.getId(), result.getId());
    }

    @Test
    void buscarPorId_noExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(id));
    }

    @Test
    void buscarPorCodigoRastreo_retornaLista() {
        when(repo.buscarPorCodigoRastreo("RC2026")).thenReturn(List.of(paquete));

        List<Paquete> result = service.buscarPorCodigoRastreo("RC2026");

        assertEquals(1, result.size());
    }

    @Test
    void buscarPorSucursalYEstado_retornaLista() {
        when(repo.buscarPorSucursalYEstado("LIMA", EstadoPaquete.REGISTRADO))
            .thenReturn(List.of(paquete));

        List<Paquete> result = service.buscarPorSucursalYEstado("LIMA", EstadoPaquete.REGISTRADO);

        assertEquals(1, result.size());
    }

    @Test
    void cambiarEstado_transicionValida_guardaPaqueteYHistorial() {
        when(repo.buscarPorId(paquete.getId())).thenReturn(Optional.of(paquete));
        when(repo.guardar(any(Paquete.class))).thenReturn(paquete);

        service.cambiarEstado(paquete.getId(), EstadoPaquete.EN_ALMACEN, "operador1");

        assertEquals(EstadoPaquete.EN_ALMACEN, paquete.getEstadoActual());
        verify(repo).guardar(paquete);

        ArgumentCaptor<EstadoHistorial> historialCaptor = ArgumentCaptor.forClass(EstadoHistorial.class);
        verify(historial).guardar(historialCaptor.capture());
        assertEquals(EstadoPaquete.EN_ALMACEN, historialCaptor.getValue().getEstado());
        assertEquals("operador1", historialCaptor.getValue().getUsuarioResponsable());
    }

    @Test
    void cambiarEstado_transicionInvalida_lanzaTransicionInvalidaException() {
        when(repo.buscarPorId(paquete.getId())).thenReturn(Optional.of(paquete));

        assertThrows(TransicionInvalidaException.class,
            () -> service.cambiarEstado(paquete.getId(), EstadoPaquete.ENTREGADO, "operador1"));

        verify(repo, never()).guardar(any());
    }

    @Test
    void cambiarEstado_paqueteNoExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.cambiarEstado(id, EstadoPaquete.EN_ALMACEN, "operador1"));
    }

    @Test
    void obtenerHistorial_paqueteExiste_retornaLista() {
        UUID paqueteId = UUID.randomUUID();
        List<EstadoHistorial> historialEsperado = List.of(
            new EstadoHistorial(null, paqueteId, EstadoPaquete.REGISTRADO, OffsetDateTime.now(), "sistema"),
            new EstadoHistorial(null, paqueteId, EstadoPaquete.EN_ALMACEN, OffsetDateTime.now(), "operador1")
        );

        when(repo.buscarPorId(paqueteId)).thenReturn(Optional.of(paquete));
        when(historial.obtenerPorPaqueteId(paqueteId)).thenReturn(historialEsperado);

        List<EstadoHistorial> result = service.obtenerHistorial(paqueteId);

        assertEquals(2, result.size());
        assertEquals(EstadoPaquete.REGISTRADO, result.get(0).getEstado());
        assertEquals(EstadoPaquete.EN_ALMACEN, result.get(1).getEstado());
    }

    @Test
    void obtenerHistorial_paqueteNoExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerHistorial(id));
    }

    @Test
    void buscarPorRemitenteOrDestinatario_conClientes_retornaPaquetes() {
        ClienteReferencia cliente = new ClienteReferencia(remitenteId, "12345678", "Juan Pérez", "juan@test.com");
        when(clienteFeign.buscarPorNombre("Juan")).thenReturn(List.of(cliente));
        when(repo.buscarPorRemitenteIdOrDestinatarioId(Set.of(remitenteId))).thenReturn(List.of(paquete));

        List<Paquete> result = service.buscarPorRemitenteOrDestinatario("Juan");

        assertEquals(1, result.size());
        assertEquals(paquete.getId(), result.get(0).getId());
    }

    @Test
    void buscarPorRemitenteOrDestinatario_sinClientes_retornaListaVacia() {
        when(clienteFeign.buscarPorNombre("Inexistente")).thenReturn(List.of());

        List<Paquete> result = service.buscarPorRemitenteOrDestinatario("Inexistente");

        assertTrue(result.isEmpty());
        verify(repo, never()).buscarPorRemitenteIdOrDestinatarioId(any());
    }

    @Test
    void actualizar_paqueteExiste_modificaYRecalculaTarifa() {
        when(repo.buscarPorId(paquete.getId())).thenReturn(Optional.of(paquete));
        when(repo.guardar(any(Paquete.class))).thenReturn(paquete);
        when(tarifaCalculator.calcular(7.5, 200.0, "LIMA", "AREQUIPA")).thenReturn(45.0);

        PaqueteActualizarRequest request = new PaqueteActualizarRequest(7.5, 200.0, "LIMA", "AREQUIPA");
        Paquete result = service.actualizar(paquete.getId(), request);

        assertEquals(7.5, result.getPesoKg());
        assertEquals(200.0, result.getValorDeclarado());
        assertEquals(45.0, result.getTarifa());
        verify(repo).guardar(paquete);
    }

    @Test
    void actualizar_paqueteNoExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.empty());

        PaqueteActualizarRequest request = new PaqueteActualizarRequest(7.5, 200.0, null, null);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(id, request));
        verify(repo, never()).guardar(any());
    }

    @Test
    void eliminar_paqueteExiste_elimina() {
        when(repo.buscarPorId(paquete.getId())).thenReturn(Optional.of(paquete));

        service.eliminar(paquete.getId());

        verify(repo).eliminar(paquete.getId());
    }

    @Test
    void eliminar_paqueteNoExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(id));
        verify(repo, never()).eliminar(any());
    }

    @Test
    void asignarCategoria_happyPath_agregaCategoria() {
        when(repo.buscarPorId(paquete.getId())).thenReturn(Optional.of(paquete));

        Categoria nuevaCategoria = new Categoria(UUID.randomUUID(), "PELIGROSO", "Mercancía peligrosa");
        when(categoriaRepo.buscarPorId(nuevaCategoria.getId())).thenReturn(Optional.of(nuevaCategoria));
        when(repo.guardar(any(Paquete.class))).thenReturn(paquete);

        service.asignarCategoria(paquete.getId(), nuevaCategoria.getId());

        assertTrue(paquete.getCategorias().contains(nuevaCategoria));
        verify(repo).guardar(paquete);
    }

    @Test
    void asignarCategoria_paqueteNoExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.asignarCategoria(id, UUID.randomUUID()));
        verify(repo, never()).guardar(any());
    }

    @Test
    void asignarCategoria_categoriaNoExiste_lanzaResourceNotFoundException() {
        UUID catId = UUID.randomUUID();
        when(repo.buscarPorId(paquete.getId())).thenReturn(Optional.of(paquete));
        when(categoriaRepo.buscarPorId(catId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.asignarCategoria(paquete.getId(), catId));
        verify(repo, never()).guardar(any());
    }

    @Test
    void asignarCategoria_categoriaYaAsignada_lanzaConflictException() {
        when(repo.buscarPorId(paquete.getId())).thenReturn(Optional.of(paquete));
        when(categoriaRepo.buscarPorId(categoriaBase.getId())).thenReturn(Optional.of(categoriaBase));

        assertThrows(ConflictException.class,
            () -> service.asignarCategoria(paquete.getId(), categoriaBase.getId()));
            verify(repo, never()).guardar(any());
    }
}
