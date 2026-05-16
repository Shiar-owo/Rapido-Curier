package com.rapidocurier.authservice.domain.model;

import java.util.Set;
import java.util.UUID;

public class Usuario {
    private final UUID id;
    private String nombre;
    private String password;
    private String email;
    private Set<String> roles;

    public Usuario(String nombre, String password, String email, Set<String> roles) {
        this.id = UUID.randomUUID();
        this.nombre = nombre;
        this.password = password;
        this.email = email;
        this.roles = roles;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}