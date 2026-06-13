package com.rapidocurier.paquetesservice.application.port.in;

import java.util.UUID;

public interface EliminarPaqueteUseCase {
    void eliminar(UUID id);
}
