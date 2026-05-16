package com.rapidocurier.authservice.domain.port.out;

import com.rapidocurier.authservice.domain.model.Usuario;

import java.util.Optional;

public interface UsuarioRepositoryPort {
    Optional<Usuario> buscarPorEmail(String email);
    boolean existePorEmail(String email);
    Usuario guardar(Usuario usuario);
}