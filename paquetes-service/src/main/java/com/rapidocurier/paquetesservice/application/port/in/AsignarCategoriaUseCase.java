package com.rapidocurier.paquetesservice.application.port.in;

import java.util.UUID;

public interface AsignarCategoriaUseCase {
    void asignarCategoria(UUID paqueteId, UUID categoriaId);
}
