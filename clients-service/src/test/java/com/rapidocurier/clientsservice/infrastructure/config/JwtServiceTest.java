package com.rapidocurier.clientsservice.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
    }

    @Test
    void validarToken_tokenValido_retornaClaims() {
        String userId = UUID.randomUUID().toString();
        String token = Jwts.builder()
            .subject(userId)
            .claim("nombre", "Juan Pérez")
            .claim("email", "juan@test.com")
            .claim("roles", "ADMIN,OPERADOR")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        Claims claims = jwtService.validarToken(token);

        assertAll(
            () -> assertEquals(userId, claims.getSubject()),
            () -> assertEquals("Juan Pérez", claims.get("nombre")),
            () -> assertEquals("juan@test.com", claims.get("email")),
            () -> assertEquals("ADMIN,OPERADOR", claims.get("roles"))
        );
    }

    @Test
    void validarToken_tokenInvalido_lanzaExcepcion() {
        assertThrows(JwtService.JwtTokenInvalidoException.class,
            () -> jwtService.validarToken("token-invalido"));
    }

    @Test
    void validarToken_tokenExpirado_lanzaExcepcion() {
        String token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("roles", "CLIENTE")
            .issuedAt(new Date(System.currentTimeMillis() - 7200000))
            .expiration(new Date(System.currentTimeMillis() - 3600000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertThrows(JwtService.JwtTokenInvalidoException.class,
            () -> jwtService.validarToken(token));
    }
}
