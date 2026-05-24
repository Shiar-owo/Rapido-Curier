package com.rapidocurier.clientsservice.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key";

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_conTokenValido_seteaAuthentication() throws Exception {
        String userId = UUID.randomUUID().toString();
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(generarTokenValido(userId, "ADMIN,OPERADOR"))
            .getPayload();

        when(jwtService.validarToken("token-valido")).thenReturn(claims);

        request.addHeader("Authorization", "Bearer token-valido");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(userId, auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OPERADOR")));
        assertEquals(2, auth.getAuthorities().size());
    }

    @Test
    void doFilter_sinToken_noSeteaAuthentication() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_tokenInvalido_limpiaAuthentication() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "previo", null, java.util.List.of()));

        when(jwtService.validarToken("token-malo"))
            .thenThrow(new JwtService.JwtTokenInvalidoException("Token inválido"));

        request.addHeader("Authorization", "Bearer token-malo");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_siempreLlamaFilterChain() throws Exception {
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_conTokenValido_siempreLlamaFilterChain() throws Exception {
        String userId = UUID.randomUUID().toString();
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(generarTokenValido(userId, "ADMIN"))
            .getPayload();
        when(jwtService.validarToken("token-valido")).thenReturn(claims);

        request.addHeader("Authorization", "Bearer token-valido");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_headerSinBearer_noSeteaAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic somecreds");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_conTokenValido_sinRoles_seteaAuthentication() throws Exception {
        String userId = UUID.randomUUID().toString();
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(generarTokenValido(userId, null))
            .getPayload();

        when(jwtService.validarToken("token-sin-roles")).thenReturn(claims);

        request.addHeader("Authorization", "Bearer token-sin-roles");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(userId, auth.getPrincipal());
        assertTrue(auth.getAuthorities().isEmpty());
    }

    private String generarTokenValido(String subject, String roles) {
        var builder = Jwts.builder()
            .subject(subject)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)));
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.compact();
    }
}
