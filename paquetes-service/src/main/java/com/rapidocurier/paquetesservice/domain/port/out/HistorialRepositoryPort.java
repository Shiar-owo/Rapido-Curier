package com.rapidocurier.paquetesservice.domain.port.out;

import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;

import java.util.List;
import java.util.UUID;

public interface HistorialRepositoryPort {
    void guardar(EstadoHistorial historial);
    List<EstadoHistorial> obtenerPorPaqueteId(UUID paqueteId);
}
