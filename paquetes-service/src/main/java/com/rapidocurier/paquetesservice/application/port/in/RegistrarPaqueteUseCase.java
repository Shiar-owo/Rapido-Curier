package com.rapidocurier.paquetesservice.application.port.in;

import com.rapidocurier.paquetesservice.domain.model.Paquete;

public interface RegistrarPaqueteUseCase {
    Paquete registrar(PaqueteRequest request);
}
