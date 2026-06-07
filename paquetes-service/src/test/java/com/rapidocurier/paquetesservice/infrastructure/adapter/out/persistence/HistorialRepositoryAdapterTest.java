package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.paquetesservice.TestcontainersConfiguration;
import com.rapidocurier.paquetesservice.domain.model.Categoria;
import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;
import com.rapidocurier.paquetesservice.domain.port.out.CategoriaRepositoryPort;
import com.rapidocurier.paquetesservice.domain.port.out.HistorialRepositoryPort;
import com.rapidocurier.paquetesservice.domain.port.out.PaqueteRepositoryPort;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper.CategoriaMapperImpl;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper.HistorialEstadoMapperImpl;
import com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.mapper.PaqueteMapperImpl;
import com.rapidocurier.paquetesservice.infrastructure.config.JpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaConfig.class, HistorialRepositoryAdapter.class, HistorialEstadoMapperImpl.class,
         PaqueteRepositoryAdapter.class, PaqueteMapperImpl.class, CategoriaRepositoryAdapter.class, CategoriaMapperImpl.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.config.import=",
    "spring.cloud.vault.enabled=false",
    "spring.cloud.config.enabled=false"
})
@Transactional
class HistorialRepositoryAdapterTest {

    @Autowired
    private HistorialRepositoryPort repository;

    @Autowired
    private PaqueteRepositoryPort paqueteRepository;

    @Autowired
    private CategoriaRepositoryPort categoriaRepository;

    private Paquete paqueteGuardado;

    @BeforeEach
    void setUp() {
        Categoria categoria = categoriaRepository.guardar(new Categoria(null, "HIST_CAT_" + UUID.randomUUID(), "Test"));
        paqueteGuardado = paqueteRepository.guardar(
            Paquete.create(
                UUID.randomUUID(), UUID.randomUUID(),
                5.0, 100.0, "LIMA", "AREQUIPA", 27.0,
                Set.of(categoria)
            )
        );
    }

    @Test
    void guardar_and_obtenerPorPaqueteId_returnsHistorial() {
        EstadoHistorial historial = new EstadoHistorial(
            null, paqueteGuardado.getId(),
            EstadoPaquete.REGISTRADO, OffsetDateTime.now(), "sistema"
        );

        repository.guardar(historial);

        List<EstadoHistorial> resultado = repository.obtenerPorPaqueteId(paqueteGuardado.getId());

        assertEquals(1, resultado.size());
        assertEquals(EstadoPaquete.REGISTRADO, resultado.get(0).getEstado());
        assertEquals("sistema", resultado.get(0).getUsuarioResponsable());
        assertEquals(paqueteGuardado.getId(), resultado.get(0).getPaqueteId());
    }

    @Test
    void guardar_multiplesEstados_orderedByFechaAsc() {
        repository.guardar(new EstadoHistorial(
            null, paqueteGuardado.getId(),
            EstadoPaquete.REGISTRADO, OffsetDateTime.now().minusHours(3), "sistema"
        ));
        repository.guardar(new EstadoHistorial(
            null, paqueteGuardado.getId(),
            EstadoPaquete.EN_ALMACEN, OffsetDateTime.now().minusHours(2), "operador1"
        ));
        repository.guardar(new EstadoHistorial(
            null, paqueteGuardado.getId(),
            EstadoPaquete.EN_TRANSITO, OffsetDateTime.now().minusHours(1), "operador2"
        ));

        List<EstadoHistorial> resultado = repository.obtenerPorPaqueteId(paqueteGuardado.getId());

        assertEquals(3, resultado.size());
        assertEquals(EstadoPaquete.REGISTRADO, resultado.get(0).getEstado());
        assertEquals(EstadoPaquete.EN_ALMACEN, resultado.get(1).getEstado());
        assertEquals(EstadoPaquete.EN_TRANSITO, resultado.get(2).getEstado());
    }

    @Test
    void obtenerPorPaqueteId_noHistorial_returnsEmptyList() {
        UUID paqueteIdSinHistorial = UUID.randomUUID();

        List<EstadoHistorial> resultado = repository.obtenerPorPaqueteId(paqueteIdSinHistorial);

        assertTrue(resultado.isEmpty());
    }
}
