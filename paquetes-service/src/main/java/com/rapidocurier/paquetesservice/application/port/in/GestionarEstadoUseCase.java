package com.rapidocurier.paquetesservice.application.port.in;

import com.rapidocurier.paquetesservice.domain.model.EstadoHistorial;
import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;

import java.util.List;
import java.util.UUID;

public interface GestionarEstadoUseCase {
    void cambiarEstado(UUID paqueteId, EstadoPaquete nuevoEstado, String usuarioResponsable);
    List<EstadoHistorial> obtenerHistorial(UUID paqueteId);
}
