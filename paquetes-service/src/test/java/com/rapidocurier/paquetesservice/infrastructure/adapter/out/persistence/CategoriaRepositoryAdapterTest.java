package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.paquetesservice.TestcontainersConfiguration;
import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.port.out.CategoriaRepositoryPort;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper.CategoriaMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({TestcontainersConfiguration.class, CategoriaRepositoryAdapter.class, CategoriaMapperImpl.class})
@ActiveProfiles("test")
class CategoriaRepositoryAdapterTest {

    @Autowired
    private CategoriaRepositoryPort repository;

    @Test
    void guardar_and_buscarPorId_returnsCategoria() {
        Categoria categoria = new Categoria(null, "FRAGIL", "Artículo frágil");

        Categoria guardada = repository.guardar(categoria);

        Optional<Categoria> encontrada = repository.buscarPorId(guardada.getId());

        assertTrue(encontrada.isPresent());
        assertEquals("FRAGIL", encontrada.get().getNombre());
        assertEquals("Artículo frágil", encontrada.get().getDescripcion());
    }

    @Test
    void listarTodas_returnsAllCategorias() {
        repository.guardar(new Categoria(null, "DOC_" + UUID.randomUUID(), "Documento 1"));
        repository.guardar(new Categoria(null, "DOC_" + UUID.randomUUID(), "Documento 2"));

        List<Categoria> todas = repository.listarTodas();

        assertTrue(todas.size() >= 2);
    }

    @Test
    void buscarPorId_noExiste_returnsEmpty() {
        Optional<Categoria> resultado = repository.buscarPorId(UUID.randomUUID());

        assertFalse(resultado.isPresent());
    }
}
