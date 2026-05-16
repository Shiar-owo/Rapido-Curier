package com.rapidocurier.authservice.application.service;

import com.rapidocurier.authservice.domain.exception.CredencialesInvalidasException;
import com.rapidocurier.authservice.domain.exception.ConflictException;
import com.rapidocurier.authservice.domain.model.Usuario;
import com.rapidocurier.authservice.domain.port.out.JwtPort;
import com.rapidocurier.authservice.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarios;

    @Mock
    private JwtPort jwt;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_exitoso_retornaToken() {
        when(usuarios.buscarPorEmail("test@example.com")).thenReturn(Optional.of(
                new Usuario("Test User", "$2a$encoded", "test@example.com", Set.of("CLIENTE"))));
        when(encoder.matches("password123", "$2a$encoded")).thenReturn(true);
        when(jwt.generarToken(any())).thenReturn("jwt.token.here");

        String result = authService.login("test@example.com", "password123");

        assertEquals("jwt.token.here", result);
        verify(jwt).generarToken(any());
    }

    @Test
    void login_usuarioNoExiste_lanzaExcepcion() {
        when(usuarios.buscarPorEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login("nonexistent@example.com", "password123"));
    }

    @Test
    void login_contrasenaIncorrecta_lanzaExcepcion() {
        when(usuarios.buscarPorEmail("test@example.com")).thenReturn(Optional.of(
                new Usuario("Test User", "$2a$encoded", "test@example.com", Set.of("CLIENTE"))));
        when(encoder.matches("wrongpassword", "$2a$encoded")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login("test@example.com", "wrongpassword"));
    }

    @Test
    void register_exitoso_creaUsuarioConRolParametro() {
        when(usuarios.existePorEmail("new@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("$2a$encoded");
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return new Usuario(u.getNombre(), u.getPassword(), u.getEmail(), u.getRoles());
        });

        Usuario result = authService.registrar("New User", "new@example.com", "password123", "ADMIN");

        assertEquals("New User", result.getNombre());
        assertEquals("new@example.com", result.getEmail());
        assertTrue(result.getRoles().contains("ADMIN"));
        verify(usuarios).guardar(any(Usuario.class));
    }

    @Test
    void register_emailDuplicado_lanzaConflictException() {
        when(usuarios.existePorEmail("existing@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.registrar("Existing User", "existing@example.com", "password123", "CLIENTE"));
    }
}