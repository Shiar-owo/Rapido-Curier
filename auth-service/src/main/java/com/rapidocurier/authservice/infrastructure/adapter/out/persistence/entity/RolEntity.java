package com.rapidocurier.authservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre", nullable = false, unique = true, length = 20)
    @Enumerated(EnumType.STRING)
    private RolNombreEntity nombre;

    public enum RolNombreEntity {
        ADMIN, OPERADOR, CLIENTE
    }
}