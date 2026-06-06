package com.rapidocurier.paquetesservice.application.service;

import com.rapidocurier.paquetesservice.application.port.in.ConsultarPaqueteUseCase;
import com.rapidocurier.paquetesservice.application.port.in.GestionarEstadoUseCase;
import com.rapidocurier.paquetesservice.application.port.in.PaqueteRequest;
import com.rapidocurier.paquetesservice.application.port.in.RegistrarPaqueteUseCase;
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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaqueteService implements RegistrarPaqueteUseCase,
                                       ConsultarPaqueteUseCase,
                                       GestionarEstadoUseCase {

    private final PaqueteRepositoryPort repo;
    private final HistorialRepositoryPort historial;
    private final CategoriaRepositoryPort categoriaRepo;
    private final ClienteFeignPort clienteFeign;
    private final TarifaCalculator tarifaCalculator;

    public PaqueteService(PaqueteRepositoryPort repo,
                          HistorialRepositoryPort historial,
                          CategoriaRepositoryPort categoriaRepo,
                          ClienteFeignPort clienteFeign,
                          TarifaCalculator tarifaCalculator) {
        this.repo = repo;
        this.historial = historial;
        this.categoriaRepo = categoriaRepo;
        this.clienteFeign = clienteFeign;
        this.tarifaCalculator = tarifaCalculator;
    }

    @Override
    @Transactional
    public Paquete registrar(PaqueteRequest request) {
        if (request.categoriaIds() == null || request.categoriaIds().isEmpty()) {
            throw new ConflictException("El paquete debe tener al menos una categoría");
        }

        Set<Categoria> categorias = new HashSet<>();
        for (UUID catId : request.categoriaIds()) {
            Categoria cat = categoriaRepo.buscarPorId(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + catId));
            categorias.add(cat);
        }

        Paquete paquete = Paquete.create(
            request.remitenteId(),
            request.destinatarioId(),
            request.pesoKg(),
            request.valorDeclarado(),
            request.sucursalOrigen(),
            request.sucursalDestino(),
            0.0,
            categorias
        );

        double tarifa = tarifaCalculator.calcular(
            request.pesoKg(),
            request.valorDeclarado(),
            request.sucursalOrigen(),
            request.sucursalDestino()
        );
        paquete.setTarifa(tarifa);

        Paquete guardado = repo.guardar(paquete);

        historial.guardar(new EstadoHistorial(
            null, guardado.getId(),
            EstadoPaquete.REGISTRADO,
            OffsetDateTime.now(), "sistema"
        ));

        return guardado;
    }

    @Override
    public Paquete buscarPorId(UUID id) {
        return repo.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado: " + id));
    }

    @Override
    public List<Paquete> buscarPorCodigoRastreo(String texto) {
        return repo.buscarPorCodigoRastreo(texto);
    }

    @Override
    public List<Paquete> buscarPorSucursalYEstado(String sucursal, EstadoPaquete estado) {
        return repo.buscarPorSucursalYEstado(sucursal, estado);
    }

    @Override
    public List<Paquete> buscarPorRemitenteOrDestinatario(String nombre) {
        List<ClienteReferencia> clientes = clienteFeign.buscarPorNombre(nombre);

        if (clientes.isEmpty()) {
            return List.of();
        }

        Set<UUID> clienteIds = clientes.stream()
            .map(ClienteReferencia::id)
            .collect(Collectors.toSet());

        return repo.buscarPorRemitenteIdOrDestinatarioId(clienteIds);
    }

    @Override
    @Transactional
    public void cambiarEstado(UUID paqueteId, EstadoPaquete nuevoEstado, String usuarioResponsable) {
        Paquete paquete = repo.buscarPorId(paqueteId)
            .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado: " + paqueteId));

        EstadoPaquete estadoActual = paquete.getEstadoActual();

        if (!estadoActual.esValida(nuevoEstado)) {
            throw new TransicionInvalidaException(estadoActual, nuevoEstado);
        }

        paquete.setEstadoActual(nuevoEstado);
        repo.guardar(paquete);

        historial.guardar(new EstadoHistorial(
            null, paqueteId,
            nuevoEstado,
            OffsetDateTime.now(), usuarioResponsable
        ));
    }

    @Override
    public List<EstadoHistorial> obtenerHistorial(UUID paqueteId) {
        repo.buscarPorId(paqueteId)
            .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado: " + paqueteId));

        return historial.obtenerPorPaqueteId(paqueteId);
    }
}
