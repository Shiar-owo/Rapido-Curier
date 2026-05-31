package com.rapidocurier.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidocurier.apigateway.infrastructure.common.ApiResponse;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
            || path.startsWith("/actuator/")
            || path.contains("/v3/api-docs")
            || path.startsWith("/swagger-ui/")
            || path.startsWith("/swagger-ui.html")
            || path.startsWith("/webjars/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("Token JWT requerido"));
            return;
        }

        try {
            Claims claims = jwtService.validarToken(token);
            String userId = claims.getSubject();
            String roles = claims.get("roles", String.class);

            var authorities = new ArrayList<SimpleGrantedAuthority>();
            if (roles != null) {
                Arrays.stream(roles.split(","))
                    .map(String::trim)
                    .forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
            }

            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equals(name)) return userId;
                    if ("X-User-Roles".equals(name)) return roles;
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
                    names.add("X-User-Id");
                    names.add("X-User-Roles");
                    return Collections.enumeration(names);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("X-User-Id".equals(name)) {
                        return Collections.enumeration(List.of(userId));
                    }
                    if ("X-User-Roles".equals(name)) {
                        return Collections.enumeration(roles != null ? List.of(roles) : List.of());
                    }
                    return super.getHeaders(name);
                }
            };

            filterChain.doFilter(wrappedRequest, response);

        } catch (JwtService.JwtTokenInvalidoException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error(e.getMessage()));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
