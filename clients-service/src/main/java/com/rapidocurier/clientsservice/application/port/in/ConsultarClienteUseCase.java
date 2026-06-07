package com.rapidocurier.clientsservice.application.port.in;

import com.rapidocurier.clientsservice.domain.model.Cliente;

import java.util.List;
import java.util.UUID;

public interface ConsultarClienteUseCase {
    Cliente buscarPorId(UUID id);
    List<Cliente> listarTodos();
    List<Cliente> buscarPorNombre(String nombre);
    void eliminar(UUID id);
}