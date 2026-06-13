package com.rapidocurier.paquetesservice.application.port.in;

import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.model.Paquete;

import java.util.List;
import java.util.UUID;

public interface ConsultarMisPaquetesUseCase {
    List<Paquete> buscarMisPaquetes(UUID clienteId);
    Paquete obtenerMisPaquetePorId(UUID clienteId, UUID paqueteId);
    List<EstadoHistorial> obtenerHistorialMisPaquetes(UUID clienteId, UUID paqueteId);
}
