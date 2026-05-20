package com.rapidocurier.clientsservice.domain.port.out;

import com.rapidocurier.clientsservice.domain.model.ReniecDataClient;

public interface ReniecPort {
    ReniecDataClient obtenerDatos(String dni);
}