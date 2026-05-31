package com.rapidocurier.apigateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(JwtAuthenticationFilterTest.TestController.class)
class JwtAuthenticationFilterTest {

    @LocalServerPort
    private int port;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    @RestController
    static class TestController {

        @GetMapping("/api/v1/auth/login")
        String publicEndpoint() {
            return "public";
        }

        @GetMapping("/api/v1/clientes")
        String protectedEndpoint() {
            return "protected";
        }

        @GetMapping("/actuator/health")
        String healthEndpoint() {
            return "health";
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void publicRouteSinToken_retorna200() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/v1/auth/login"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actuatorRouteSinToken_retorna200() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/health"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void protectedRouteSinToken_retorna401() {
        assertThatThrownBy(() -> restTemplate.getForEntity(url("/api/v1/clientes"), String.class))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(e -> {
                var ex = (HttpClientErrorException) e;
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            });
    }

    @Test
    void protectedRouteTokenInvalido_retorna401() {
        RequestEntity<Void> request = RequestEntity
            .get(URI.create(url("/api/v1/clientes")))
            .header("Authorization", "Bearer token-malo")
            .build();
        assertThatThrownBy(() -> restTemplate.exchange(request, String.class))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(e -> {
                var ex = (HttpClientErrorException) e;
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            });
    }

    @Test
    void protectedRouteHeaderSinBearer_retorna401() {
        RequestEntity<Void> request = RequestEntity
            .get(URI.create(url("/api/v1/clientes")))
            .header("Authorization", "Basic somecreds")
            .build();
        assertThatThrownBy(() -> restTemplate.exchange(request, String.class))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(e -> {
                var ex = (HttpClientErrorException) e;
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            });
    }

    @Test
    void protectedRouteTokenValido_retorna200() {
        String token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("roles", "ADMIN")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        RequestEntity<Void> request = RequestEntity
            .get(URI.create(url("/api/v1/clientes")))
            .header("Authorization", "Bearer " + token)
            .build();
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
