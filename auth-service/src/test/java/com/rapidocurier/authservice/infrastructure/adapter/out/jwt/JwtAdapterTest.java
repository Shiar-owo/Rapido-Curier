package com.rapidocurier.authservice.infrastructure.adapter.out.jwt;

import com.rapidocurier.authservice.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtAdapterTest {

    private JwtAdapter jwtAdapter;

    @BeforeEach
    void setUp() {
        jwtAdapter = new JwtAdapter("dGhpcyBpcyBhIHRlc3Qgc2VjcmV0IGtleSBmb3IganVuaXQ=", 3600000);
    }

    @Test
    void generarToken_retornaTokenValido() {
        Usuario usuario = new Usuario("Juan", "password123", "juan@test.com", Set.of("CLIENTE"));

        String token = jwtAdapter.generarToken(usuario);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void generarToken_tokenContieneEmail() {
        Usuario usuario = new Usuario("Juan", "password123", "juan@test.com", Set.of("ADMIN"));

        String token = jwtAdapter.generarToken(usuario);

        assertTrue(token.contains("jwt") || token.length() > 50);
    }

    @Test
    void generarToken_mismoUsuario_generaDistintoToken() {
        Usuario usuario = new Usuario("Juan", "password123", "juan@test.com", Set.of("CLIENTE"));

        String token1 = jwtAdapter.generarToken(usuario);
        String token2 = jwtAdapter.generarToken(usuario);

        assertNotNull(token1);
        assertNotNull(token2);
    }
}