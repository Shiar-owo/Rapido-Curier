package com.rapidocurier.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterUnitTest {

    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key";

    @Mock
    private JwtService jwtService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FilterChain filterChain;

    @Captor
    private ArgumentCaptor<HttpServletRequest> requestCaptor;

    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void doFilter_tokenValido_agregaXUserIdHeader() throws Exception {
        String userId = UUID.randomUUID().toString();
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(generarToken(userId, "ADMIN"))
            .getPayload();

        when(jwtService.validarToken("token-valido")).thenReturn(claims);

        request.setRequestURI("/api/v1/clientes");
        request.addHeader("Authorization", "Bearer token-valido");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        HttpServletRequest wrapped = requestCaptor.getValue();

        assertInstanceOf(HttpServletRequestWrapper.class, wrapped);
        assertEquals(userId, wrapped.getHeader("X-User-Id"));
    }

    @Test
    void doFilter_tokenValido_agregaXUserRolesHeader() throws Exception {
        String userId = UUID.randomUUID().toString();
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(generarToken(userId, "ADMIN,OPERADOR"))
            .getPayload();

        when(jwtService.validarToken("token-multirol")).thenReturn(claims);

        request.setRequestURI("/api/v1/clientes");
        request.addHeader("Authorization", "Bearer token-multirol");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        HttpServletRequest wrapped = requestCaptor.getValue();

        assertEquals("ADMIN,OPERADOR", wrapped.getHeader("X-User-Roles"));
    }

    @Test
    void doFilter_tokenValido_sinRoles_rolesHeaderEsNull() throws Exception {
        String userId = UUID.randomUUID().toString();
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(generarToken(userId, null))
            .getPayload();

        when(jwtService.validarToken("token-sin-roles")).thenReturn(claims);

        request.setRequestURI("/api/v1/clientes");
        request.addHeader("Authorization", "Bearer token-sin-roles");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        HttpServletRequest wrapped = requestCaptor.getValue();

        assertEquals(userId, wrapped.getHeader("X-User-Id"));
        assertNull(wrapped.getHeader("X-User-Roles"));
    }

    private String generarToken(String subject, String roles) {
        var builder = Jwts.builder()
            .subject(subject)
            .claim("roles", roles)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)));
        return builder.compact();
    }
}
