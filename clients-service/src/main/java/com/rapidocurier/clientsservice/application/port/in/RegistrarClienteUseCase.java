package com.rapidocurier.clientsservice.application.port.in;

import com.rapidocurier.clientsservice.domain.model.Cliente;

public interface RegistrarClienteUseCase {
    Cliente registrar(String dni, String email);
}