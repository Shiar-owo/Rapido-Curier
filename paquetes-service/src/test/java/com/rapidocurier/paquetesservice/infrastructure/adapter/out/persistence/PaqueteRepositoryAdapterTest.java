package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.paquetesservice.TestcontainersConfiguration;
import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.domain.port.out.CategoriaRepositoryPort;
import com.rapidocurier.paquetesservice.domain.port.out.PaqueteRepositoryPort;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper.CategoriaMapperImpl;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper.PaqueteMapperImpl;
import com.rapidocurier.paquetesservice.infrastructure.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaConfig.class, PaqueteRepositoryAdapter.class, PaqueteMapperImpl.class, CategoriaRepositoryAdapter.class, CategoriaMapperImpl.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.config.import=",
    "spring.cloud.vault.enabled=false",
    "spring.cloud.config.enabled=false"
})
@Transactional
class PaqueteRepositoryAdapterTest {

    @Autowired
    private PaqueteRepositoryPort repository;

    @Autowired
    private CategoriaRepositoryPort categoriaRepository;

    private Categoria crearCategoria(String nombre) {
        return categoriaRepository.guardar(new Categoria(null, nombre, "Test"));
    }

    private Paquete crearPaquete(String sucursalOrigen, String sucursalDestino, EstadoPaquete estado) {
        Categoria cat = crearCategoria("CAT_" + UUID.randomUUID());
        Paquete paquete = Paquete.create(
            UUID.randomUUID(), UUID.randomUUID(),
            5.0, 100.0,
            sucursalOrigen, sucursalDestino, 27.0,
            Set.of(cat)
        );
        paquete.setEstadoActual(estado);
        return paquete;
    }

    @Test
    void guardar_and_buscarPorId_returnsPaquete() {
        Paquete paquete = crearPaquete("LIMA", "AREQUIPA", EstadoPaquete.REGISTRADO);

        Paquete guardado = repository.guardar(paquete);

        Optional<Paquete> encontrado = repository.buscarPorId(guardado.getId());

        assertTrue(encontrado.isPresent());
        assertEquals("LIMA", encontrado.get().getSucursalOrigen());
        assertEquals("AREQUIPA", encontrado.get().getSucursalDestino());
        assertEquals(EstadoPaquete.REGISTRADO, encontrado.get().getEstadoActual());
    }

    @Test
    void guardar_and_buscarPorCodigoRastreo_returnsPaquete() {
        Paquete paquete = crearPaquete("LIMA", "CUSCO", EstadoPaquete.REGISTRADO);
        Paquete guardado = repository.guardar(paquete);

        List<Paquete> resultados = repository.buscarPorCodigoRastreo(guardado.getCodigoRastreo());

        assertEquals(1, resultados.size());
        assertEquals(guardado.getCodigoRastreo(), resultados.get(0).getCodigoRastreo());
    }

    @Test
    void buscarPorSucursalYEstado_returnsMatchingPaquetes() {
        repository.guardar(crearPaquete("LIMA", "AREQUIPA", EstadoPaquete.REGISTRADO));
        repository.guardar(crearPaquete("LIMA", "CUSCO", EstadoPaquete.EN_ALMACEN));
        repository.guardar(crearPaquete("AREQUIPA", "LIMA", EstadoPaquete.REGISTRADO));

        List<Paquete> resultados = repository.buscarPorSucursalYEstado("LIMA", EstadoPaquete.REGISTRADO);

        assertTrue(resultados.size() >= 1);
        assertTrue(resultados.stream().allMatch(p -> p.getEstadoActual() == EstadoPaquete.REGISTRADO));
        assertTrue(resultados.stream().allMatch(p ->
            p.getSucursalOrigen().equals("LIMA") || p.getSucursalDestino().equals("LIMA")));
    }

    @Test
    void buscarPorRemitenteIdOrDestinatarioId_returnsMatchingPaquetes() {
        UUID remitenteId = UUID.randomUUID();
        UUID destinatarioId = UUID.randomUUID();
        Categoria cat = crearCategoria("REM_DEST_CAT");

        Paquete paquete1 = Paquete.create(
            remitenteId, UUID.randomUUID(),
            3.0, 50.0, "LIMA", "AREQUIPA", 15.0,
            Set.of(cat)
        );
        Paquete paquete2 = Paquete.create(
            UUID.randomUUID(), destinatarioId,
            2.0, 30.0, "CUSCO", "LIMA", 20.0,
            Set.of(cat)
        );
        repository.guardar(paquete1);
        repository.guardar(paquete2);

        List<Paquete> resultados = repository.buscarPorRemitenteIdOrDestinatarioId(Set.of(remitenteId, destinatarioId));

        assertEquals(2, resultados.size());
    }

    @Test
    void guardar_categoriasPersistidas() {
        Paquete paquete = crearPaquete("LIMA", "AREQUIPA", EstadoPaquete.REGISTRADO);
        Paquete guardado = repository.guardar(paquete);

        Optional<Paquete> encontrado = repository.buscarPorId(guardado.getId());

        assertTrue(encontrado.isPresent());
        assertEquals(1, encontrado.get().getCategorias().size());
        assertTrue(encontrado.get().getCategorias().iterator().next().getNombre().startsWith("CAT_"));
    }

    @Test
    void guardar_estadoPersistidoComoString() {
        Paquete paquete = crearPaquete("LIMA", "AREQUIPA", EstadoPaquete.EN_TRANSITO);
        Paquete guardado = repository.guardar(paquete);

        Optional<Paquete> encontrado = repository.buscarPorId(guardado.getId());

        assertTrue(encontrado.isPresent());
        assertEquals(EstadoPaquete.EN_TRANSITO, encontrado.get().getEstadoActual());
    }

    @Test
    void eliminar_removesPaquete() {
        Paquete paquete = crearPaquete("LIMA", "AREQUIPA", EstadoPaquete.REGISTRADO);
        Paquete guardado = repository.guardar(paquete);

        repository.eliminar(guardado.getId());

        Optional<Paquete> encontrado = repository.buscarPorId(guardado.getId());
        assertFalse(encontrado.isPresent());
    }
}
