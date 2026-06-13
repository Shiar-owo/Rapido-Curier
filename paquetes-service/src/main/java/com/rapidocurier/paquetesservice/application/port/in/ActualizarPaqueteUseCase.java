package com.rapidocurier.paquetesservice.application.port.in;

import com.rapidocurier.paquetesservice.domain.model.Paquete;

import java.util.UUID;

public interface ActualizarPaqueteUseCase {
    Paquete actualizar(UUID id, PaqueteActualizarRequest request);
}
