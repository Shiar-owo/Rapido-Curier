package com.rapidocurier.authservice.infrastructure.adapter.out.persistence;

import com.rapidocurier.authservice.TestcontainersConfiguration;
import com.rapidocurier.authservice.application.port.in.LoginUseCase;
import com.rapidocurier.authservice.application.port.in.RegisterUseCase;
import com.rapidocurier.authservice.domain.exception.ConflictException;
import com.rapidocurier.authservice.domain.exception.CredencialesInvalidasException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthServiceIntegrationTest {

    @Autowired
    private RegisterUseCase registerUseCase;

    @Autowired
    private LoginUseCase loginUseCase;

    @Test
    void registro_exitoso_creaUsuario() {
        var usuario = registerUseCase.registrar("Juan", "juan@test.com", "password123", "CLIENTE");

        assertNotNull(usuario);
        assertEquals("Juan", usuario.getNombre());
        assertEquals("juan@test.com", usuario.getEmail());
        assertTrue(usuario.getRoles().contains("CLIENTE"));
        assertNotNull(usuario.getId());
    }

    @Test
    void login_exitoso_retornaToken() {
        registerUseCase.registrar("Maria", "maria@test.com", "password123", "ADMIN");

        String token = loginUseCase.login("maria@test.com", "password123");

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void registro_emailDuplicado_lanzaExcepcion() {
        registerUseCase.registrar("Pedro", "pedro@test.com", "password123", "CLIENTE");

        assertThrows(ConflictException.class,
                () -> registerUseCase.registrar("Pedro Duplicado", "pedro@test.com", "password456", "CLIENTE"));
    }

    @Test
    void login_credencialesInvalidas_lanzaExcepcion() {
        assertThrows(CredencialesInvalidasException.class,
                () -> loginUseCase.login("noexiste@test.com", "password123"));
    }
}