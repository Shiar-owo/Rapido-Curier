package com.rapidocurier.clientsservice.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret:default-secret-key-minimum-32-chars}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims validarToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenInvalidoException("Token JWT inválido: " + e.getMessage());
        }
    }

    public static class JwtTokenInvalidoException extends RuntimeException {
        public JwtTokenInvalidoException(String message) {
            super(message);
        }
    }
}
