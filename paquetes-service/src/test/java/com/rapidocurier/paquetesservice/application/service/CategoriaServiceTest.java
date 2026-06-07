package com.rapidocurier.paquetesservice.application.service;

import com.rapidocurier.paquetesservice.domain.exception.ConflictException;
import com.rapidocurier.paquetesservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.port.out.CategoriaRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepositoryPort categoriaRepo;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaService(categoriaRepo);
    }

    @Test
    void crear_happyPath_guardaCategoria() {
        Categoria guardada = new Categoria(UUID.randomUUID(), "FRAGIL", "Articulo fragil");
        when(categoriaRepo.listarTodas()).thenReturn(List.of());
        when(categoriaRepo.guardar(any(Categoria.class))).thenReturn(guardada);

        Categoria result = service.crear("FRAGIL", "Articulo fragil");

        assertAll(
            () -> assertEquals("FRAGIL", result.getNombre()),
            () -> assertEquals("Articulo fragil", result.getDescripcion()),
            () -> assertNotNull(result.getId())
        );
        verify(categoriaRepo).guardar(any(Categoria.class));
    }

    @Test
    void crear_nombreDuplicado_lanzaConflictException() {
        Categoria existente = new Categoria(UUID.randomUUID(), "FRAGIL", "Articulo fragil");
        when(categoriaRepo.listarTodas()).thenReturn(List.of(existente));

        assertThrows(ConflictException.class,
            () -> service.crear("fragil", "Articulo fragil nueva"));
        verify(categoriaRepo, never()).guardar(any());
    }

    @Test
    void crear_nombreDuplicadoCaseInsensitive_lanzaConflictException() {
        Categoria existente = new Categoria(UUID.randomUUID(), "DOCUMENTO", "Documento");
        when(categoriaRepo.listarTodas()).thenReturn(List.of(existente));

        assertThrows(ConflictException.class,
            () -> service.crear("Documento", "Otra descripcion"));
        verify(categoriaRepo, never()).guardar(any());
    }

    @Test
    void listarTodas_conCategorias_retornaLista() {
        List<Categoria> categorias = List.of(
            new Categoria(UUID.randomUUID(), "FRAGIL", "Articulo fragil"),
            new Categoria(UUID.randomUUID(), "DOCUMENTO", "Documento")
        );
        when(categoriaRepo.listarTodas()).thenReturn(categorias);

        List<Categoria> result = service.listarTodas();

        assertEquals(2, result.size());
        assertEquals("FRAGIL", result.get(0).getNombre());
        assertEquals("DOCUMENTO", result.get(1).getNombre());
    }

    @Test
    void listarTodas_sinCategorias_retornaListaVacia() {
        when(categoriaRepo.listarTodas()).thenReturn(List.of());

        List<Categoria> result = service.listarTodas();

        assertTrue(result.isEmpty());
    }

    @Test
    void buscarPorId_existe_retornaCategoria() {
        UUID id = UUID.randomUUID();
        Categoria categoria = new Categoria(id, "FRAGIL", "Articulo fragil");
        when(categoriaRepo.buscarPorId(id)).thenReturn(Optional.of(categoria));

        Categoria result = service.buscarPorId(id);

        assertAll(
            () -> assertEquals(id, result.getId()),
            () -> assertEquals("FRAGIL", result.getNombre()),
            () -> assertEquals("Articulo fragil", result.getDescripcion())
        );
    }

    @Test
    void buscarPorId_noExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categoriaRepo.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(id));
    }
}
