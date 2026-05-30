    package com.rapidocurier.clientsservice.application.service;

import com.rapidocurier.clientsservice.application.port.in.ConsultarClienteUseCase;
import com.rapidocurier.clientsservice.application.port.in.RegistrarClienteUseCase;
import com.rapidocurier.clientsservice.domain.exception.ConflictException;
import com.rapidocurier.clientsservice.domain.exception.ExternalServiceException;
import com.rapidocurier.clientsservice.domain.exception.ResourceNotFoundException;
import com.rapidocurier.clientsservice.domain.model.Cliente;
import com.rapidocurier.clientsservice.domain.model.ReniecDataClient;
import com.rapidocurier.clientsservice.domain.port.out.ClienteRepositoryPort;
import com.rapidocurier.clientsservice.domain.port.out.ReniecPort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClienteService implements RegistrarClienteUseCase, ConsultarClienteUseCase {

    private final ClienteRepositoryPort repositoryPort;
    private final ReniecPort reniecPort;

    public ClienteService(ClienteRepositoryPort repositoryPort, ReniecPort reniecPort) {
        this.repositoryPort = repositoryPort;
        this.reniecPort = reniecPort;
    }

    @Override
    @Transactional
    public Cliente registrar(String dni, String email) {
        if (repositoryPort.buscarPorEmail(email).isPresent()) {
            throw new ConflictException("El email ya está registrado");
        }
        if (repositoryPort.buscarPorDni(dni).isPresent()) {
            throw new ConflictException("El DNI ya está registrado");
        }

        ReniecDataClient reniecData = reniecPort.obtenerDatos(dni);

        Cliente cliente = Cliente.create(
            dni,
            reniecData.firstName(),
            reniecData.firstLastName(),
            reniecData.secondLastName(),
            email
        );

        return repositoryPort.guardar(cliente);
    }

    @Override
    public Cliente buscarPorId(UUID id) {
        return repositoryPort.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    @Override
    public List<Cliente> listarTodos() {
        return repositoryPort.listarTodos();
    }

    @Override
    public void eliminar(UUID id) {
        if (repositoryPort.buscarPorId(id).isEmpty()) {
            throw new ResourceNotFoundException("Cliente no encontrado");
        }
        repositoryPort.eliminar(id);
    }
}