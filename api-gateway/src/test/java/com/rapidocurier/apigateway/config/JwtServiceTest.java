package com.rapidocurier.apigateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = JwtService.class)
@ActiveProfiles("test")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret}")
    private String jwtSecret;

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
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
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
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertThrows(JwtService.JwtTokenInvalidoException.class,
            () -> jwtService.validarToken(token));
    }
}
