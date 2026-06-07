package com.rapidocurier.clientsservice.domain.port.out;

import com.rapidocurier.clientsservice.domain.model.Cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepositoryPort {
    Cliente guardar(Cliente cliente);
    Optional<Cliente> buscarPorId(UUID id);
    Optional<Cliente> buscarPorDni(String dni);
    Optional<Cliente> buscarPorEmail(String email);
    List<Cliente> listarTodos();
    List<Cliente> buscarPorNombre(String nombre);
    void eliminar(UUID id);
}