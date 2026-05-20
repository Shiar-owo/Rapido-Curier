package com.rapidocurier.clientsservice.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Cliente {
    private UUID id;
    private String dni;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String email;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private Cliente(
        UUID id, String dni, String nombre,
        String apellidoPaterno, String apellidoMaterno,
        String email, OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.dni = dni;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Cliente create(
            String dni, String nombre, String apellidoPaterno,
            String apellidoMaterno, String email
        ) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Cliente(
            UUID.randomUUID(),
            dni,
            nombre,
            apellidoPaterno,
            apellidoMaterno,
            email,
            now,
            now
        );
    }

    public static Cliente rehydrate(
            UUID id, String dni,String nombre,
            String apellidoPaterno, String apellidoMaterno,
            String email, OffsetDateTime createdAt, OffsetDateTime updatedAt
        ) {
        return new Cliente(
            id,
            dni,
            nombre,
            apellidoPaterno,
            apellidoMaterno,
            email,
            createdAt,
            updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }

    public String getNombreCompleto() {
        return String.format("%s %s %s", nombre, apellidoPaterno, apellidoMaterno);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}