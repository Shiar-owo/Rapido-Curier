package com.rapidocurier.paquetesservice.domain.port.out;

import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PaqueteRepositoryPort {
    Paquete guardar(Paquete paquete);
    Optional<Paquete> buscarPorId(UUID id);
    List<Paquete> buscarPorCodigoRastreo(String texto);
    List<Paquete> buscarPorSucursalYEstado(String sucursal, EstadoPaquete estado);
    List<Paquete> buscarPorRemitenteIdOrDestinatarioId(Set<UUID> clienteIds);
    List<Paquete> buscarPorClienteId(UUID clienteId);
    List<Paquete> buscarPorCategoriaNombre(String nombreCategoria);
    void eliminar(UUID id);
}
