package com.rapidocurier.paquetesservice.domain.port.out;

import com.rapidocurier.paquetesservice.domain.model.ClienteReferencia;

import java.util.List;
import java.util.UUID;

public interface ClienteFeignPort {
    ClienteReferencia obtenerCliente(UUID id);
    List<ClienteReferencia> buscarPorNombre(String nombre);
}
