package com.rapidocurier.authservice.application.service;

import com.rapidocurier.authservice.application.port.in.LoginUseCase;
import com.rapidocurier.authservice.application.port.in.RegisterUseCase;
import com.rapidocurier.authservice.domain.exception.CredencialesInvalidasException;
import com.rapidocurier.authservice.domain.exception.ConflictException;
import com.rapidocurier.authservice.domain.model.Usuario;
import com.rapidocurier.authservice.domain.port.out.JwtPort;
import com.rapidocurier.authservice.domain.port.out.UsuarioRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService implements LoginUseCase, RegisterUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final JwtPort jwt;
    private final PasswordEncoder encoder;

    public AuthService(UsuarioRepositoryPort usuarios, JwtPort jwt, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.jwt = jwt;
        this.encoder = encoder;
    }

    @Override
    public String login(String email, String password) {
        Usuario u = usuarios.buscarPorEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));
        if (!encoder.matches(password, u.getPassword())) {
            throw new CredencialesInvalidasException("Contraseña incorrecta");
        }
        return jwt.generarToken(u);
    }

    @Override
    public Usuario registrar(String nombre, String email, String password, String rol) {
        if (usuarios.existePorEmail(email)) {
            throw new ConflictException("El email ya está registrado");
        }
        Usuario nuevo = new Usuario(nombre, encoder.encode(password), email, Set.of(rol));
        return usuarios.guardar(nuevo);
    }
}