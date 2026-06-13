package com.rapidocurier.paquetesservice.application.port.in;

import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;
import com.rapidocurier.paquetesservice.domain.model.Paquete;

import java.util.List;
import java.util.UUID;

public interface ConsultarPaqueteUseCase {
    Paquete buscarPorId(UUID id);
    List<Paquete> buscarPorCodigoRastreo(String texto);
    List<Paquete> buscarPorSucursalYEstado(String sucursal, EstadoPaquete estado);
    List<Paquete> buscarPorRemitenteOrDestinatario(String nombre);
    List<Paquete> buscarPorCategoriaNombre(String nombreCategoria);
}
